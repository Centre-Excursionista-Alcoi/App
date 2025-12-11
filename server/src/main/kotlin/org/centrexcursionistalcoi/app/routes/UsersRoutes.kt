package org.centrexcursionistalcoi.app.routes

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.centrexcursionistalcoi.app.ADMIN_GROUP_NAME
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.*
import org.centrexcursionistalcoi.app.database.table.UserReferences
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.respondError
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.assertAdmin
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSessionOrFail
import org.jetbrains.exposed.v1.core.eq

fun Route.usersRoutes() {
    // Provides a list of all users - admin only
    get("/users") {
        assertAdmin() ?: return@get

        val departmentMembers = Database { DepartmentMemberEntity.all().map { it.toData() } }
        val lendingUsers = Database { LendingUserEntity.all().map { it.toData() } }
        val insurances = Database { UserInsuranceEntity.all().map { it.toData() } }
        val users = Database {
            UserReferenceEntity.all().map { user ->
                user.toData(
                    lendingUser = lendingUsers.find { it.sub == user.sub.value },
                    insurances = insurances.filter { it.userSub == user.sub.value },
                    departments = departmentMembers.filter { it.userSub == user.sub.value }
                )
            }
        }

        call.respond(users)
    }
    // Promote a user to admin - admin only
    post("/users/{sub}/promote") {
        assertAdmin() ?: return@post

        val sub = call.parameters["sub"]!!

        // Find user reference
        val reference = Database { UserReferenceEntity.find { UserReferences.sub eq sub }.firstOrNull() }
        if (reference == null) {
            respondError(Error.UserNotFound())
            return@post
        }

        Database {
            // Add admin group to user
            val groups = reference.groups.toMutableList()
            if (!groups.contains(ADMIN_GROUP_NAME)) {
                groups.add(ADMIN_GROUP_NAME)
                reference.groups = groups
            }
        }

        call.respond(HttpStatusCode.NoContent)
    }
    get("/members") {
        val session = getUserSessionOrFail() ?: return@get

        var members = Database { MemberEntity.all().map { it.toMember() } }

        // Non-admins get stripped member data
        if (!session.isAdmin()) {
            members = members.map { it.strip() }
        }

        call.respond(members)
    }
}
