package org.centrexcursionistalcoi.app.endpoints

import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.endpoints.model.Endpoint

object RootEndpoint: Endpoint("/") {
    override suspend fun RoutingContext.body() {
        respondSuccess()
    }
}
