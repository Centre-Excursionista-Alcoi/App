package org.centrexcursionistalcoi.app.endpoints.auth

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.server.response.Errors

object DeleteUserEndpoint: SecureEndpoint("/users/{id}", HttpMethod.Delete) {
    override suspend fun RoutingContext.secureBody(user: User) {
        if (!user.isAdmin) {
            respondFailure(Errors.Forbidden)
            return
        }

        // Check if the user exists
        val id = call.parameters["id"] ?: run {
            respondFailure(Errors.InvalidRequest)
            return
        }
        val confirmUser = ServerDatabase { User.findById(id) }
        if (confirmUser == null) {
            respondFailure(Errors.UserNotFound)
            return
        }

        // Confirm it
        ServerDatabase { user.delete() }

        respondSuccess(HttpStatusCode.Accepted)
    }
}
