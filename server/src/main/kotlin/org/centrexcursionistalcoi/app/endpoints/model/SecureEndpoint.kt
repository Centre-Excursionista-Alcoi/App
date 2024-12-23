package org.centrexcursionistalcoi.app.endpoints.model

import io.ktor.http.HttpMethod
import io.ktor.server.routing.RoutingContext
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import org.centrexcursionistalcoi.app.database.SessionsDatabase
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.database.utils.findUserById
import org.centrexcursionistalcoi.app.security.UserSession
import org.centrexcursionistalcoi.app.server.response.Errors
import org.slf4j.LoggerFactory

abstract class SecureEndpoint(
    route: String,
    httpMethod: HttpMethod = HttpMethod.Post
) : Endpoint(route, httpMethod) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun RoutingContext.body() {
        logger.info("Getting session...")
        val session = call.sessions.get(UserSession::class)
        if (session == null) {
            logger.error("Not logged in")
            respondFailure(Errors.NotLoggedIn)
            return
        }

        logger.info("Validating session...")
        val isSessionValid = SessionsDatabase.validateSession(session)
        if (!isSessionValid) {
            logger.error("Session not valid")
            respondFailure(Errors.InvalidSession)
            return
        }

        logger.info("Finding user...")
        val user = findUserById(session.email)
        if (user == null) {
            logger.error("User not found")
            respondFailure(Errors.UserNotFound)
            return
        }

        logger.info("Securing body...")
        secureBody(user)
    }

    protected abstract suspend fun RoutingContext.secureBody(user: User)
}
