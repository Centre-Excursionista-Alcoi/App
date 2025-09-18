package org.centrexcursionistalcoi.app.plugins

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.Application
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.sessions
import io.ktor.utils.io.copyTo
import io.ktor.utils.io.streams.asByteWriteChannel
import org.centrexcursionistalcoi.app.Greeting
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.Department
import org.centrexcursionistalcoi.app.database.entity.File
import org.centrexcursionistalcoi.app.database.entity.Post
import org.centrexcursionistalcoi.app.database.table.Departments
import org.centrexcursionistalcoi.app.database.table.Files
import org.centrexcursionistalcoi.app.database.table.Posts
import org.centrexcursionistalcoi.app.database.utils.encodeEntityListToString
import org.centrexcursionistalcoi.app.database.utils.encodeEntityToString
import org.centrexcursionistalcoi.app.database.utils.findBy
import org.centrexcursionistalcoi.app.database.utils.get
import org.centrexcursionistalcoi.app.database.utils.getAll
import org.centrexcursionistalcoi.app.database.utils.insert
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSession
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSessionOrFail
import org.centrexcursionistalcoi.app.utils.toUUID
import org.jetbrains.exposed.v1.core.eq
import java.io.ByteArrayOutputStream

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Ktor: ${Greeting().greet()}")
        }

        get("/download/{uuid}") {
            val uuid = call.parameters["uuid"]?.toUUID()
            if (uuid == null) {
                return@get call.respondText("Missing or malformed uuid", status = HttpStatusCode.BadRequest)
            }

            val file = Database { File.findBy { Files.id eq uuid } }
            if (file == null) {
                return@get call.respondText("File not found", status = HttpStatusCode.NotFound)
            }

            val session = getUserSession()
            if (file.rules?.canBeReadBy(session) == false) {
                return@get call.respondText("You don't have permission to access this file", status = HttpStatusCode.Forbidden)
            }

            call.respondBytes(
                contentType = file.type?.let(ContentType::parse)
            ) { file.data }
        }

        get("/dashboard") {
            val session = getUserSessionOrFail() ?: return@get
            call.respondText("Welcome ${session.username}! Email: ${session.email}")
        }

        get("/departments") {
            val departments = Database { Department.getAll() }

            call.respondText(ContentType.Application.Json) {
                json.encodeEntityListToString(departments)
            }
        }
        get("/departments/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                return@get call.respondText("Malformed id", status = HttpStatusCode.BadRequest)
            }

            val department = Database { Department.findBy { Departments.id eq id } }
            if (department == null) {
                return@get call.respondText("Department #$id not found", status = HttpStatusCode.NotFound)
            }

            call.respondText(ContentType.Application.Json) {
                json.encodeEntityToString(department)
            }
        }
        post("/departments") {
            val session = getUserSessionOrFail() ?: return@post
            if (!session.isAdmin()) {
                call.respondText("You don't have permission to create departments", status = HttpStatusCode.Forbidden)
                return@post
            }

            if (!call.request.contentType().match(ContentType.MultiPart.FormData)) {
                call.respondText("Content-Type must be multipart/form-data", status = HttpStatusCode.BadRequest)
                return@post
            }

            var displayName: String? = null
            var contentType: ContentType? = null
            var originalFileName: String? = null
            val imageDataStream = ByteArrayOutputStream()

            val formParameters = call.receiveMultipart()
            formParameters.forEachPart { partData ->
                when (partData) {
                    is PartData.FormItem -> {
                        if (partData.name == "displayName") {
                            displayName = partData.value
                        }
                    }
                    is PartData.FileItem -> {
                        if (partData.name == "image") {
                            contentType = partData.contentType
                            originalFileName = partData.originalFileName
                            partData.provider().copyTo(imageDataStream.asByteWriteChannel())
                        }
                    }
                    else -> { /* nothing */ }
                }
            }

            if (displayName == null) {
                call.respondText("Missing displayName", status = HttpStatusCode.BadRequest)
                return@post
            }

            val department = Database {
                val imageFile = File.insert {
                    it[Files.name] = originalFileName
                    it[Files.type] = contentType?.toString() ?: "application/octet-stream"
                    it[Files.data] = imageDataStream.toByteArray()
                }

                Department.insert {
                    it[Departments.displayName] = displayName
                    it[Departments.imageFile] = imageFile.id
                }
            }

            call.response.header(HttpHeaders.Location, "/departments/${department.id.value}")
            call.respondText("Department created", status = HttpStatusCode.Created)
        }

        get("/posts") {
            val session = getUserSession()
            val isLoggedIn = session != null
            val posts = Database {
                if (isLoggedIn)
                    Post.getAll()
                else
                    Post.get { Posts.onlyForMembers eq false }
            }.toMutableList()
            if (session == null) {
                // Not logged in, filter out members-only posts
                posts.removeIf { it.onlyForMembers }
            }

            call.respondText(ContentType.Application.Json) {
                json.encodeEntityListToString(posts)
            }
        }

        get("/logout") {
            call.sessions.clear<UserSession>()
            call.respondText("OK")
        }
    }
}
