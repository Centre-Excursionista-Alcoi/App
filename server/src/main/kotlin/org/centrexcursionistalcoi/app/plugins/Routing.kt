package org.centrexcursionistalcoi.app.plugins

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.sessions
import org.centrexcursionistalcoi.app.Greeting
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.Department
import org.centrexcursionistalcoi.app.database.entity.File
import org.centrexcursionistalcoi.app.database.entity.Post
import org.centrexcursionistalcoi.app.database.table.Files
import org.centrexcursionistalcoi.app.database.table.Posts
import org.centrexcursionistalcoi.app.database.utils.encodeListToString
import org.centrexcursionistalcoi.app.database.utils.findBy
import org.centrexcursionistalcoi.app.database.utils.get
import org.centrexcursionistalcoi.app.database.utils.getAll
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSession
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSessionOrRedirect
import org.centrexcursionistalcoi.app.utils.toUUID
import org.jetbrains.exposed.v1.core.eq

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
                contentType = ContentType.parse(file.type)
            ) { file.data }
        }

        get("/dashboard") {
            val session = getUserSessionOrRedirect() ?: return@get
            call.respondText("Welcome ${session.username}! Email: ${session.email}")
        }

        get("/departments") {
            val departments = Database { Department.getAll() }

            call.respondText(ContentType.Application.Json) {
                json.encodeListToString(departments)
            }
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
                json.encodeListToString(posts)
            }
        }

        get("/logout") {
            call.sessions.clear<UserSession>()
            call.respondText("OK")
        }
    }
}
