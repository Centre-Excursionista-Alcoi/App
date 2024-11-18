package org.centrexcursionistalcoi.app.endpoints.status

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.endpoints.model.Endpoint

object PingEndpoint: Endpoint("/ping") {
    override suspend fun RoutingContext.body() {
        call.respondText("pong", status = HttpStatusCode.OK)
    }
}
