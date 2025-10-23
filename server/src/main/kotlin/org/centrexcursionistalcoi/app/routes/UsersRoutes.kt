package org.centrexcursionistalcoi.app.routes

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentMemberEntity
import org.centrexcursionistalcoi.app.database.entity.LendingUserEntity
import org.centrexcursionistalcoi.app.database.entity.UserInsuranceEntity
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.database.table.DepartmentMembers
import org.centrexcursionistalcoi.app.database.table.LendingUsers
import org.centrexcursionistalcoi.app.database.table.UserInsurances
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSessionOrFail
import org.jetbrains.exposed.v1.core.eq

fun Route.usersRoutes() {
    // Provides a list of all users - admin only
    get("/users") {
        val session = getUserSessionOrFail() ?: return@get
        if (!session.isAdmin()) {
            // Return only self
            val lendingUser = Database { LendingUserEntity.find { LendingUsers.userSub eq session.sub }.firstOrNull()?.toData() }
            val insurances = Database { UserInsuranceEntity.find { UserInsurances.userSub eq session.sub }.map { it.toData() } }
            val departments = Database { DepartmentMemberEntity.find { DepartmentMembers.userSub eq session.sub }.map { it.toData() } }
            val self = Database { UserReferenceEntity.getOrProvide(session).toData(lendingUser, insurances, departments) }

            call.respond(listOf(self))
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
}
