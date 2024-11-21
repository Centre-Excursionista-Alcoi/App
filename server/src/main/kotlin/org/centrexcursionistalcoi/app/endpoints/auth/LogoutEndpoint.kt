package org.centrexcursionistalcoi.app.endpoints.auth

import io.ktor.server.routing.RoutingContext
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import org.centrexcursionistalcoi.app.database.SessionsDatabase
import org.centrexcursionistalcoi.app.endpoints.model.Endpoint
import org.centrexcursionistalcoi.app.security.UserSession

object LogoutEndpoint: Endpoint("/logout") {
    override suspend fun RoutingContext.body() {
        val session = call.sessions.get(UserSession::class)
        if (session != null) {
            SessionsDatabase.deleteSession(session)
            call.sessions.clear<UserSession>()
        }

        respondSuccess()
    }
}
