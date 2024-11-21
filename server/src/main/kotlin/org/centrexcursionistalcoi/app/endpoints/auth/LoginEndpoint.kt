package org.centrexcursionistalcoi.app.endpoints.auth

import io.ktor.server.routing.RoutingContext
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.SessionsDatabase
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.model.BasicAuthEndpoint
import org.centrexcursionistalcoi.app.security.Passwords
import org.centrexcursionistalcoi.app.security.UserSession
import org.centrexcursionistalcoi.app.server.response.Errors

object LoginEndpoint : BasicAuthEndpoint("/login") {
    override suspend fun RoutingContext.secureBody(username: String, password: String) {
        // Check that the user is not already logged in
        val currentSession = call.sessions.get(UserSession::class)
        if (currentSession != null) {
            SessionsDatabase.deleteSession(currentSession)
            call.sessions.clear<UserSession>()
        }

        // Find the user
        val user = ServerDatabase { User.findById(username.lowercase()) }
        if (user == null) {
            respondFailure(Errors.WrongCredentials)
            return
        }

        // Check that it's confirmed
        if (!user.confirmed) {
            respondFailure(Errors.UserNotConfirmed)
            return
        }

        // Validate its password
        val (salt, hash) = user.salt to user.hash
        val passwordCorrect = Passwords.isExpectedPassword(password, salt, hash)
        if (!passwordCorrect) {
            respondFailure(Errors.WrongCredentials)
            return
        }

        // Create the session
        val session = SessionsDatabase.newSession(username, call.request.local.remoteAddress)

        // Set the session
        call.sessions.set(session, UserSession::class)

        respondSuccess()
    }
}
