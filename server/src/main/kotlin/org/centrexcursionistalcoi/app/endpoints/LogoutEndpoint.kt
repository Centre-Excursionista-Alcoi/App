package org.centrexcursionistalcoi.app.endpoints

import io.ktor.server.routing.RoutingContext
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.sessions
import org.centrexcursionistalcoi.app.endpoints.model.Endpoint
import org.centrexcursionistalcoi.app.security.UserSession

object LogoutEndpoint: Endpoint("/logout") {
    override suspend fun RoutingContext.body() {
        call.sessions.clear<UserSession>()
        respondSuccess()
    }
}
