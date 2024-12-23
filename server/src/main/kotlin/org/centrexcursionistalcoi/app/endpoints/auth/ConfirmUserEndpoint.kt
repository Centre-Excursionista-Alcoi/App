package org.centrexcursionistalcoi.app.endpoints.auth

import io.ktor.http.HttpMethod
import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.database.utils.findUserById
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.server.response.Errors

object ConfirmUserEndpoint : SecureEndpoint("/users/{id}/confirm", HttpMethod.Post) {
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
        val confirmUser = findUserById(id)
        if (confirmUser == null) {
            respondFailure(Errors.UserNotFound)
            return
        }

        // Confirm it
        ServerDatabase("ConfirmUserEndpoint", "confirmUser") { user.confirmed = true }

        respondSuccess()
    }
}
