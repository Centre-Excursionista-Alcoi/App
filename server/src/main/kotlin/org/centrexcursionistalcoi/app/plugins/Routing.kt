package org.centrexcursionistalcoi.app.plugins

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.sessions
import org.centrexcursionistalcoi.app.Greeting
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.database.entity.LendingUserEntity
import org.centrexcursionistalcoi.app.database.table.LendingUsers
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSession
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSessionOrFail
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.routes.departmentsRoutes
import org.centrexcursionistalcoi.app.routes.postsRoutes
import org.centrexcursionistalcoi.app.utils.toUUID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

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

            val file = Database { FileEntity.findById(uuid) }
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

        get("/profile") {
            val session = getUserSessionOrFail() ?: return@get

            val lendingUser = Database {
                LendingUserEntity.find { LendingUsers.userSub eq session.sub }.firstOrNull()?.toData()
            }

            call.respond(
                ProfileResponse(session.username, session.email, session.groups, lendingUser)
            )
        }
        post("/profile/lendingSignUp") {
            val session = getUserSessionOrFail() ?: return@post
            try {
                val contentType = call.request.contentType()
                if (!contentType.match(ContentType.Application.FormUrlEncoded)) {
                    call.respondText("Content-Type must be form url-encoded. It was: $contentType", status = HttpStatusCode.BadRequest)
                    return@post
                }

                val parameters = call.receiveParameters()
                val nif = parameters["nif"]
                val phoneNumber = parameters["phoneNumber"]

                if (nif.isNullOrBlank()) {
                    call.respondText("NIF is required", status = HttpStatusCode.BadRequest)
                    return@post
                }
                if (phoneNumber.isNullOrBlank()) {
                    call.respondText("Phone number is required", status = HttpStatusCode.BadRequest)
                    return@post
                }

                Database {
                    LendingUserEntity.new {
                        userSub = session.sub
                        this.nif = nif
                        this.phoneNumber = phoneNumber
                    }
                }

                call.respondText("OK", status = HttpStatusCode.Created)
            } catch (e: ExposedSQLException) {
                if (e.message?.contains("unique index", true) == true) {
                    call.respondText("User already signed up", status = HttpStatusCode.Conflict)
                } else {
                    call.respondText("Database error: ${e.message}", status = HttpStatusCode.InternalServerError)
                }
            }
        }

        departmentsRoutes()
        postsRoutes()

        get("/logout") {
            call.sessions.clear<UserSession>()
            call.respondText("OK")
        }
    }
}
