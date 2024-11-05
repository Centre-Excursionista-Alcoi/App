package org.centrexcursionistalcoi.app.endpoints.auth

import io.ktor.server.routing.RoutingContext
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.model.BasicAuthEndpoint
import org.centrexcursionistalcoi.app.security.Passwords
import org.centrexcursionistalcoi.app.security.UserSession
import org.centrexcursionistalcoi.app.server.response.Errors

object LoginEndpoint : BasicAuthEndpoint("/login") {
    override suspend fun RoutingContext.secureBody(username: String, password: String) {
        // Find the user
        val user = ServerDatabase { User.findById(username) }
        if (user == null) {
            respondFailure(Errors.WrongCredentials)
            return
        }

        // Validate its password
        val (salt, hash) = user.salt to user.hash
        val passwordCorrect = Passwords.isExpectedPassword(password, salt, hash)
        if (!passwordCorrect) {
            respondFailure(Errors.WrongCredentials)
            return
        }

        // Set the session
        call.sessions.set(UserSession(username), UserSession::class)

        respondSuccess()
    }
}
