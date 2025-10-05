package org.centrexcursionistalcoi.app.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentMemberEntity
import org.centrexcursionistalcoi.app.database.entity.LendingUserEntity
import org.centrexcursionistalcoi.app.database.entity.UserInsuranceEntity
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSessionOrFail

fun Route.usersRoutes() {
    // Provides a list of all users - admin only
    get("/users") {
        val session = getUserSessionOrFail() ?: return@get
        if (!session.isAdmin()) {
            call.respondText("You must be an admin to access this resource", status = HttpStatusCode.Forbidden)
            return@get
        }

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
