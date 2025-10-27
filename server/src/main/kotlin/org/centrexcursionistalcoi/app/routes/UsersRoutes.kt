package org.centrexcursionistalcoi.app.routes

import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.json.JsonObject
import org.centrexcursionistalcoi.app.ADMIN_GROUP_UUID
import org.centrexcursionistalcoi.app.authentik.AuthentikUser
import org.centrexcursionistalcoi.app.authentik.errors.AuthentikError
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentMemberEntity
import org.centrexcursionistalcoi.app.database.entity.LendingUserEntity
import org.centrexcursionistalcoi.app.database.entity.UserInsuranceEntity
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.database.table.DepartmentMembers
import org.centrexcursionistalcoi.app.database.table.LendingUsers
import org.centrexcursionistalcoi.app.database.table.UserInsurances
import org.centrexcursionistalcoi.app.database.table.UserReferences
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.respondError
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.assertAdmin
import org.centrexcursionistalcoi.app.plugins.UserSession.Companion.getUserSessionOrFail
import org.centrexcursionistalcoi.app.plugins.getAuthHttpClient
import org.centrexcursionistalcoi.app.security.OIDCConfig
import org.centrexcursionistalcoi.app.serialization.bodyAsJson
import org.centrexcursionistalcoi.app.utils.toJson
import org.centrexcursionistalcoi.app.utils.toUuid
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
    // Promote a user to admin - admin only
    post("/users/{sub}/promote") {
        assertAdmin() ?: return@post

        val authentikToken = OIDCConfig.authentikToken
        if (authentikToken == null) {
            respondError(Error.AuthentikNotConfigured())
            return@post
        }

        val sub = call.parameters["sub"]!!

        // Find user reference
        val reference = Database { UserReferenceEntity.find { UserReferences.sub eq sub }.firstOrNull() }
        if (reference == null) {
            respondError(Error.UserNotFound())
            return@post
        }

        // Fetch a list of all the groups the user already has
        val url = URLBuilder(OIDCConfig.authentikBase)
            .appendPathSegments("/api/v3/core/users/${reference.pk}/")
            .build()
        val response = getAuthHttpClient().get(url) {
            bearerAuth(authentikToken)
            accept(ContentType.Application.Json)
        }
        if (!response.status.isSuccess()) {
            val error = response.bodyAsJson(AuthentikError.serializer())
            throw error.asThrowable()
        }
        val user = response.bodyAsJson(AuthentikUser.serializer())
        val groups = user.groups.toMutableSet()

        // Add the admin group
        groups.add(ADMIN_GROUP_UUID.toUuid())

        // Update the user
        val updateUrl = URLBuilder(OIDCConfig.authentikBase)
            .appendPathSegments("/api/v3/core/users/${reference.pk}/")
            .build()
        val updateResponse = getAuthHttpClient().put(updateUrl) {
            bearerAuth(authentikToken)
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(
                JsonObject(
                    mapOf(
                        // Required fields - keep existing values
                        "name" to user.name.toJson(),
                        "username" to user.username.toJson(),
                        // Updated groups
                        "groups" to groups.toJson { it.toString().toJson() },
                    )
                )
            )
        }
        if (!updateResponse.status.isSuccess()) {
            val error = updateResponse.bodyAsJson(AuthentikError.serializer())
            throw error.asThrowable()
        }

        call.respond(HttpStatusCode.NoContent)
    }
}
