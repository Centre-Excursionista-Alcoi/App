package org.centrexcursionistalcoi.app.endpoints.model

import io.ktor.http.HttpMethod
import io.ktor.server.routing.RoutingContext
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import org.centrexcursionistalcoi.app.security.UserSession
import org.centrexcursionistalcoi.app.server.response.Errors

abstract class SecureEndpoint(
    route: String,
    httpMethod: HttpMethod = HttpMethod.Post
) : Endpoint(route, httpMethod) {
    override suspend fun RoutingContext.body() {
        val session = call.sessions.get(UserSession::class)
        if (session != null) {
            secureBody(session.email)
        } else {
            respondFailure(Errors.NotLoggedIn)
        }
    }

    protected abstract suspend fun RoutingContext.secureBody(email: String)
}
