package org.centrexcursionistalcoi.app.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import org.centrexcursionistalcoi.app.ADMIN_GROUP_NAME
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentMemberEntity
import org.centrexcursionistalcoi.app.database.entity.LendingUserEntity
import org.centrexcursionistalcoi.app.database.entity.UserInsuranceEntity
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.database.op.ValueInStringArrayOp
import org.centrexcursionistalcoi.app.database.table.DepartmentMembers
import org.centrexcursionistalcoi.app.database.table.LendingUsers
import org.centrexcursionistalcoi.app.database.table.UserInsurances
import org.centrexcursionistalcoi.app.database.table.UserReferences
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.respondError
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.assertAdmin
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSessionOrFail
import org.jetbrains.exposed.v1.core.eq

fun Route.usersRoutes() {
    // Provides a list of all users - admin only
    get("/users") {
        val session = getUserSessionOrFail() ?: return@get
        if (!session.isAdmin()) {
            // Return only self, and admins
            // Admins are necessary because lendings hold references to them (e.g., who approved the lending, or who received the items)
            // However, we only provide the basic data (display name, email and groups).
            val lendingUser = Database { LendingUserEntity.find { LendingUsers.userSub eq session.sub }.firstOrNull()?.toData() }
            val insurances = Database { UserInsuranceEntity.find { UserInsurances.userSub eq session.sub }.map { it.toData() } }
            val departments = Database { DepartmentMemberEntity.find { DepartmentMembers.userSub eq session.sub }.map { it.toData() } }
            val self = Database { UserReferenceEntity[session.sub].toData(lendingUser, insurances, departments) }

            // Find all admins
            val admins = Database {
                UserReferenceEntity.find { ValueInStringArrayOp(ADMIN_GROUP_NAME, UserReferences.groups) }.map { user ->
                    user.toData(null, null, null)
                }
            }

            call.respond(listOf(self) + admins)
        } else {
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

        TODO("Implement promote user to admin functionality")

        call.respond(HttpStatusCode.NoContent)
    }
}
