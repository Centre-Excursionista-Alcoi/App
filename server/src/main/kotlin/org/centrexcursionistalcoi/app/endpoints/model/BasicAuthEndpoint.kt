package org.centrexcursionistalcoi.app.endpoints.model

import io.ktor.http.HttpMethod
import io.ktor.server.auth.UserPasswordCredential
import io.ktor.server.auth.principal
import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.server.response.Errors

abstract class BasicAuthEndpoint(
    route: String,
    httpMethod: HttpMethod = HttpMethod.Post
) : Endpoint(route, httpMethod) {
    override suspend fun RoutingContext.body() {
        val principal = call.principal<UserPasswordCredential>()
        if (principal != null) {
            secureBody(principal.name, principal.password)
        } else {
            respondFailure(Errors.InvalidCredentials)
        }
    }

    protected abstract suspend fun RoutingContext.secureBody(username: String, password: String)
}
