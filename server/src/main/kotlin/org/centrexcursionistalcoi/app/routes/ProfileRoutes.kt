package org.centrexcursionistalcoi.app.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentMemberEntity
import org.centrexcursionistalcoi.app.database.entity.LendingUserEntity
import org.centrexcursionistalcoi.app.database.table.DepartmentMembers
import org.centrexcursionistalcoi.app.database.table.LendingUsers
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSessionOrFail
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

fun Route.profileRoutes() {
    get("/profile") {
        val session = getUserSessionOrFail() ?: return@get

        val departments = Database {
            DepartmentMemberEntity.find {
                (DepartmentMembers.userSub eq session.sub) and (DepartmentMembers.confirmed eq true)
            }.map { it.department.id.value }
        }
        val lendingUser = Database {
            LendingUserEntity.find { LendingUsers.userSub eq session.sub }.firstOrNull()?.toData()
        }

        call.respond(
            ProfileResponse(session.username, session.email, session.groups, departments, lendingUser)
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
}
