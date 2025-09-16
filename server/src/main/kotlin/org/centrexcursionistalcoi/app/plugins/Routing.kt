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
import org.centrexcursionistalcoi.app.database.utils.get
import org.centrexcursionistalcoi.app.database.utils.getAll
import org.centrexcursionistalcoi.app.database.utils.list
import org.centrexcursionistalcoi.app.database.utils.serializer
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSession
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSessionOrFail
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSessionOrRedirect
import org.jetbrains.exposed.v1.core.eq

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
            val session = getUserSession()
            val isLoggedIn = session != null
            println("isLoggedIn=${isLoggedIn}")
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
                json.encodeToString(Post.serializer(Posts).list(), posts)
            }
        }

        get("/logout") {
            call.sessions.clear<UserSession>()
            call.respondText("OK")
        }
    }
}
