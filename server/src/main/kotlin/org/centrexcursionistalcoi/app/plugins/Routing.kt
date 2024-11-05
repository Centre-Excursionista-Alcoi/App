package org.centrexcursionistalcoi.app.plugins

import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import org.centrexcursionistalcoi.app.endpoints.Endpoint
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Routing")

private val endpoints = listOf<Endpoint>()

private fun Routing.configureEndpoint(endpoint: Endpoint) {
    logger.trace("${endpoint.method.value} :: ${endpoint.route}")
    when (endpoint.method) {
        HttpMethod.Get -> get(endpoint.route) { endpoint(this) }
        HttpMethod.Post -> post(endpoint.route) { endpoint(this) }
        HttpMethod.Patch -> patch(endpoint.route) { endpoint(this) }
        HttpMethod.Delete -> delete(endpoint.route) { endpoint(this) }
        else -> error("Unsupported HTTP operation: ${endpoint.method.value}")
    }
}

fun Application.configureRouting() {
    logger.info("Configuring routing plugin...")
    routing {
        logger.debug("Adding ${endpoints.size} endpoints...")
        for (endpoint in endpoints) {
            configureEndpoint(endpoint)
        }
    }
}
