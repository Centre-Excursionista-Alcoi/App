package org.centrexcursionistalcoi.app.plugins

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.sessions
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSession
import org.centrexcursionistalcoi.app.routes.departmentsRoutes
import org.centrexcursionistalcoi.app.routes.inventoryRoutes
import org.centrexcursionistalcoi.app.routes.lendingsRoutes
import org.centrexcursionistalcoi.app.routes.postsRoutes
import org.centrexcursionistalcoi.app.routes.profileRoutes
import org.centrexcursionistalcoi.app.routes.usersRoutes
import org.centrexcursionistalcoi.app.routes.webDavRoutes
import org.centrexcursionistalcoi.app.utils.toUUIDOrNull

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello! The Centre Excursionista d'Alcoi API is running.")
        }

        get("/download/{uuid}") {
            val uuid = call.parameters["uuid"]?.toUUIDOrNull()
            if (uuid == null) {
                return@get call.respondText("Missing or malformed uuid", status = HttpStatusCode.BadRequest)
            }

            val file = Database { FileEntity.findById(uuid) }
            if (file == null) {
                return@get call.respondText("File not found", status = HttpStatusCode.NotFound)
            }

            val session = getUserSession()
            if (file.rules?.canBeReadBy(session) == false) {
                return@get call.respondText(
                    "You don't have permission to access this file",
                    status = HttpStatusCode.Forbidden
                )
            }

            call.respondBytes(
                contentType = file.type?.let(ContentType::parse)
            ) { file.data }
        }

        configureAuthRoutes()

        profileRoutes()
        departmentsRoutes()
        postsRoutes()
        usersRoutes()
        inventoryRoutes()
        lendingsRoutes()

        route("/webdav") {
            webDavRoutes()
        }

        get("/logout") {
            call.sessions.clear<UserSession>()
            call.respondText("OK")
        }
    }
}
