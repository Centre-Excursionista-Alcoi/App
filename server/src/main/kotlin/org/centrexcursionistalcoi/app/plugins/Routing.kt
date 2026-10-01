package org.centrexcursionistalcoi.app.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.resources.Resources
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.centrexcursionistalcoi.app.data.ServerInfo
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.ConfigEntity
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.routes.Api
import org.centrexcursionistalcoi.app.routes.appLinkFallbackRoutes
import org.centrexcursionistalcoi.app.routes.departmentsRoutes
import org.centrexcursionistalcoi.app.routes.eventsRoutes
import org.centrexcursionistalcoi.app.routes.inventoryRoutes
import org.centrexcursionistalcoi.app.routes.lendingsRoutes
import org.centrexcursionistalcoi.app.routes.memoriesRoutes
import org.centrexcursionistalcoi.app.routes.postsRoutes
import org.centrexcursionistalcoi.app.routes.profileRoutes
import org.centrexcursionistalcoi.app.routes.qualificationsRoutes
import org.centrexcursionistalcoi.app.routes.respondAppLinkFallbackOr
import org.centrexcursionistalcoi.app.routes.robotsRoute
import org.centrexcursionistalcoi.app.routes.securityRoutes
import org.centrexcursionistalcoi.app.routes.usersRoutes
import org.centrexcursionistalcoi.app.routes.verifyRoutes
import org.centrexcursionistalcoi.app.routes.webDavRoutes
import org.centrexcursionistalcoi.app.routes.wellKnownRoutes
import org.centrexcursionistalcoi.app.security.UserSession.Companion.getUserSession
import org.centrexcursionistalcoi.app.utils.toUuidOrNull
import org.centrexcursionistalcoi.app.version
import org.centrexcursionistalcoi.app.versionCode

fun Application.configureRouting() {
    install(Resources)
    routing {
        get("/") {
            // On the app-links host (centrexcursionistalcoi.app by default), the bare domain behaves the same as
            // any other unmatched path there -- see appLinkFallbackRoutes -- rather than announcing the API.
            call.respondAppLinkFallbackOr {
                call.respondText("Hello! The Centre Excursionista d'Alcoi API is running.")
            }
        }

        get<Api.Download.Id> { downloadId ->
            val uuid = downloadId.uuid.toUuidOrNull()
            if (uuid == null) {
                return@get call.respondText("Missing or malformed uuid", status = HttpStatusCode.BadRequest)
            }

            val file = Database { FileEntity.findById(uuid) }
            if (file == null) {
                return@get call.respondText("File not found", status = HttpStatusCode.NotFound)
            }

            val session = getUserSession()
            val rules = file.rules
            if (rules == null) {
                // No rules were ever recorded for this file -- nothing in this codebase actually sets them (see
                // FileReadWriteRules), so this is the overwhelming common case, not a deliberate "public" choice.
                // Require at least a logged-in session as a safe default; per-resource-type rules restricting
                // reads further (e.g. to the owning user) are set explicitly where the file is created.
                if (session == null) {
                    return@get call.respondText(
                        "You must be logged in to access this file",
                        status = HttpStatusCode.Unauthorized
                    )
                }
            } else if (!rules.canBeReadBy(session)) {
                return@get call.respondText(
                    "You don't have permission to access this file",
                    status = HttpStatusCode.Forbidden
                )
            }

            call.respondStoredFile(file.objectKey, file.size, file.contentType, file.lastModified)
        }

        authTokenRoutes()
        rateLimit(RateLimits.AUTHENTICATION) {
            configureAuthRoutes()
        }

        configureSSERoutes()

        profileRoutes()
        securityRoutes()
        departmentsRoutes()
        eventsRoutes()
        qualificationsRoutes()
        postsRoutes()
        usersRoutes()
        inventoryRoutes()
        lendingsRoutes()
        memoriesRoutes()

        route("/webdav") {
            webDavRoutes()
        }

        robotsRoute()
        route(".well-known") {
            wellKnownRoutes()
        }

        verifyRoutes()

        appLinkFallbackRoutes()

        get<Api.Info> {
            val databaseVersion = ConfigEntity.DatabaseVersion.get() ?: 0
            val lastCEASync = ConfigEntity.LastCEASync.get()?.toEpochMilliseconds() ?: 0L

            call.respond(ServerInfo(
                version = ServerInfo.Version(
                    version = version,
                    databaseVersion = databaseVersion,
                    code = versionCode,
                ),
                lastCEASync = lastCEASync,
            ))
        }
    }
}
