package org.centrexcursionistalcoi.app.plugins

import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.sessions
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.Greeting
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.Post
import org.centrexcursionistalcoi.app.database.table.Posts
import org.centrexcursionistalcoi.app.database.utils.getAll
import org.centrexcursionistalcoi.app.database.utils.serializer
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSessionOrFail
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSessionOrRedirect

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Ktor: ${Greeting().greet()}")
        }

        get("/dashboard") {
            val session = getUserSessionOrRedirect() ?: return@get
            call.respondText("Welcome ${session.username}! Email: ${session.email}")
        }

        get("/posts") {
            getUserSessionOrFail() ?: return@get
            val posts = Database { Post.getAll() }

            call.respondText(ContentType.Application.Json) {
                json.encodeToString(ListSerializer(Post.serializer(Posts)), posts)
            }
        }

        get("/logout") {
            call.sessions.clear<UserSession>()
            call.respondText("OK")
        }
    }
}
