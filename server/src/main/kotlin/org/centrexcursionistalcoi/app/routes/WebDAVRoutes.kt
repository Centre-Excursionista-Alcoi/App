package org.centrexcursionistalcoi.app.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.basicAuthenticationCredentials
import io.ktor.server.request.header
import io.ktor.server.request.path
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.RoutingHandler
import io.ktor.server.routing.get
import io.ktor.server.routing.head
import io.ktor.server.routing.method
import io.ktor.server.routing.options
import io.ktor.server.routing.route
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import org.centrexcursionistalcoi.app.CEAWebDAVMessage
import org.centrexcursionistalcoi.app.CEAWebDAVNormalizedPath
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.fs.VirtualFileSystem
import org.centrexcursionistalcoi.app.plugins.UserSession
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSession
import org.centrexcursionistalcoi.app.plugins.login
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("WebDAV")

fun Route.propfind(body: RoutingHandler): Route {
    return method(HttpMethod("PROPFIND")) { handle(body) }
}

/**
 * Handle session authentication and authorization for WebDAV routes.
 *
 * Handles sessions and basic authentication.
 *
 * @return true if the user is authenticated and authorized, false otherwise (response is sent)
 */
private suspend fun RoutingContext.handleSession(): Boolean {
    val session = getUserSession()
    if (session != null) {
        if (session.isAdmin()) {
            return true
        } else {
            call.response.header(HttpHeaders.CEAWebDAVMessage, "You are not an admin")
            call.respond(HttpStatusCode.Forbidden)
            return false
        }
    }
    val basicAuth = call.request.basicAuthenticationCredentials()
    if (basicAuth == null) {
        call.response.header(HttpHeaders.CEAWebDAVMessage, "Missing or invalid Authorization header")
        call.response.header(HttpHeaders.WWWAuthenticate, "Basic realm=\"WebDAV Admin Area\"")
        call.respond(HttpStatusCode.Unauthorized)
        return false
    } else {
        val loginError = login(basicAuth.name, basicAuth.password.toCharArray())
        if (loginError != null) {
            call.response.header(HttpHeaders.CEAWebDAVMessage, "Invalid credentials: $loginError")
            call.respond(HttpStatusCode.Unauthorized)
            return false
        }

        val session = Database { UserSession.fromNif(basicAuth.name) }
        if (!session.isAdmin()) {
            call.response.header(HttpHeaders.CEAWebDAVMessage, "You are not an admin")
            call.respond(HttpStatusCode.Forbidden)
            return false
        }

        logger.info("WebDAV login successful for admin user (${session.sub}). Sending session cookie...")
        call.sessions.set(session)
    }
    return true
}

fun Route.webDavRoutes() {
    // catch-all route: capture the whole remainder into "path"
    route("{path...}") {
        // OPTIONS: advertise supported methods
        options {
            if (!handleSession()) return@options

            call.response.header(HttpHeaders.Allow, "OPTIONS, GET, HEAD, PROPFIND")
            call.respondText("", ContentType.Text.Plain, HttpStatusCode.OK)
        }

        // GET and HEAD for files
        get {
            if (!handleSession()) return@get

            val raw = call.pathParameters.getAll("path")?.joinToString("/") ?: ""
            val path = normalizePath(raw)
            call.response.header(HttpHeaders.CEAWebDAVNormalizedPath, path)

            // Try reading a file
            val itemData = try {
                VirtualFileSystem.read(path)
            } catch (e: Throwable) {
                logger.error("Error reading file at path: $path", e)
                null
            }

            if (itemData != null) {
                call.response.header(HttpHeaders.ContentLength, itemData.size.toString())
                call.respondBytes(itemData.data, itemData.contentType)
            } else {
                // If not a file, check if a directory exists
                val list = try {
                    VirtualFileSystem.list(path)
                } catch (e: Throwable) {
                    logger.error("Error listing directory at path: $path", e)
                    null
                }

                if (list != null) {
                    // Directory via GET: return a tiny HTML listing (useful for browsers)
                    val html = buildHtmlIndex(call.request.path(), path, list)
                    call.respondText(html, ContentType.Text.Html)
                } else {
                    call.response.header(HttpHeaders.CEAWebDAVMessage, "File or directory not found. Path: $path")
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

        head {
            if (!handleSession()) return@head

            // HEAD behaves like GET but without body
            val raw = call.pathParameters.getAll("path")?.joinToString("/") ?: ""
            val path = normalizePath(raw)
            call.response.header(HttpHeaders.CEAWebDAVNormalizedPath, path)

            val data = try {
                VirtualFileSystem.read(path)
            } catch (e: Throwable) {
                logger.error("Error reading file at path: $path", e)
                null
            }

            if (data != null) {
                call.response.header(HttpHeaders.ContentLength, data.size.toString())
                call.respond(HttpStatusCode.OK)
            } else {
                val list = try {
                    VirtualFileSystem.list(path)
                } catch (e: Throwable) {
                    logger.error("Error listing directory at path: $path", e)
                    null
                }
                if (list != null) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.response.header(HttpHeaders.CEAWebDAVMessage, "File or directory not found. Path: $path")
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

        // PROPFIND for directory listings (WebDAV clients use this)
        propfind {
            if (!handleSession()) return@propfind

            val raw = call.pathParameters.getAll("path")?.joinToString("/") ?: ""
            val path = normalizePath(raw)
            call.response.header(HttpHeaders.CEAWebDAVNormalizedPath, path)

            val depthHeader = call.request.header(HttpHeaders.Depth) ?: "1"
            val depth = when (depthHeader) {
                "0" -> 0
                else -> 1
            }

            // collect responses
            val responses = mutableListOf<VirtualFileSystem.Item>()

            // check if path is a file
            val fileData = try {
                VirtualFileSystem.read(path)
            } catch (e: Throwable) {
                logger.error("Error reading file at path: $path", e)
                null
            }

            if (fileData != null) {
                // path is a file: return info only about the file itself
                val size = fileData.size.toLong()
                responses.add(VirtualFileSystem.Item(path, path.substringAfterLast('/'), false, size, null))
            } else {
                val dirList = try {
                    VirtualFileSystem.list(path)
                } catch (e: Throwable) {
                    logger.error("Error listing directory at path: $path", e)
                    null
                }

                if (dirList == null) {
                    call.response.header(HttpHeaders.CEAWebDAVMessage, "File or directory not found. Path: $path")
                    call.respond(HttpStatusCode.NotFound)
                    return@propfind
                }

                // add the directory itself as response
                responses.add(VirtualFileSystem.Item(path, path.substringAfterLast('/'), true, null, null))

                if (depth > 0) {
                    // add immediate children
                    responses.addAll(dirList)
                }
            }

            val xml = buildMultiStatusXml(call.request.path(), path, responses)
            call.respondText(xml, ContentType.parse("application/xml; charset=utf-8"), HttpStatusCode.MultiStatus)
        }
    }
}

private fun normalizePath(raw: String): String {
    return raw.trim('/', ' ')
}

private fun buildHtmlIndex(requestPath: String, dirPath: String, list: List<VirtualFileSystem.Item>): String {
    val base = requestPath.trimEnd('/')
    val sb = StringBuilder()
    sb.append("<!doctype html><html><head><meta charset=\"utf-8\"><title>Index of ")
    sb.append(escapeHtml(base))
    sb.append("</title></head><body><h1>Index of ")
    sb.append(escapeHtml(base))
    sb.append("</h1><ul>")
    if (dirPath.isNotEmpty()) {
        val parentHref = requestPath.trimEnd('/').substringBeforeLast('/', "")
        sb.append("<li><a href=\"${escapeHtml(parentHref)}\">..</a></li>")
    }
    for (it in list) {
        val href = it.path
        val display = if (it.isDirectory) "${it.name}/" else it.name
        sb.append("<li><a href=\"${escapeHtml(href)}\">${escapeHtml(display)}</a></li>")
    }
    sb.append("</ul></body></html>")
    return sb.toString()
}

private fun encodePath(path: String): String {
    return path.split('/').joinToString("/") { java.net.URLEncoder.encode(it, "UTF-8") }
}

private fun escapeHtml(text: String): String =
    text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

private fun buildMultiStatusXml(requestPath: String, basePath: String, items: List<VirtualFileSystem.Item>): String {
    val sb = StringBuilder()
    sb.append("""<?xml version="1.0" encoding="utf-8"?>""")
    sb.append("<D:multistatus xmlns:D=\"DAV:\">")
    val rfc1123 = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC)
    val baseRequest = requestPath.trimEnd('/')
    for (it in items) {
        val href = if (it.path == basePath || basePath.isEmpty() && it.path.isEmpty()) {
            // the requested resource itself
            baseRequest.ifEmpty { "/" }
        } else {
            // child item within the requested dir -> append the child name
            val prefix = if (baseRequest.isEmpty()) "/" else "$baseRequest/"
            prefix + encodePath(it.name)
        }

        sb.append("<D:response>")
        sb.append("<D:href>").append(escapeXml(href)).append("</D:href>")
        sb.append("<D:propstat>")
        sb.append("<D:prop>")
        if (it.isDirectory) {
            sb.append("<D:resourcetype><D:collection/></D:resourcetype>")
        } else {
            sb.append("<D:resourcetype/>")
            it.size?.let { size -> sb.append("<D:getcontentlength>").append(size).append("</D:getcontentlength>") }
        }
        it.lastModified?.let { lm ->
            sb.append("<D:getlastmodified>").append(rfc1123.format(lm)).append("</D:getlastmodified>")
        }
        sb.append("</D:prop>")
        sb.append("<D:status>HTTP/1.1 200 OK</D:status>")
        sb.append("</D:propstat>")
        sb.append("</D:response>")
    }
    sb.append("</D:multistatus>")
    return sb.toString()
}

private fun escapeXml(s: String): String =
    s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")
