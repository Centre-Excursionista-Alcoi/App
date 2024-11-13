package org.centrexcursionistalcoi.app.plugins

import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import org.centrexcursionistalcoi.app.endpoints.RootEndpoint
import org.centrexcursionistalcoi.app.endpoints.auth.LoginEndpoint
import org.centrexcursionistalcoi.app.endpoints.auth.LogoutEndpoint
import org.centrexcursionistalcoi.app.endpoints.auth.RegisterEndpoint
import org.centrexcursionistalcoi.app.endpoints.auth.UserDataEndpoint
import org.centrexcursionistalcoi.app.endpoints.inventory.CreateItemEndpoint
import org.centrexcursionistalcoi.app.endpoints.inventory.CreateTypesEndpoint
import org.centrexcursionistalcoi.app.endpoints.inventory.ListItemsEndpoint
import org.centrexcursionistalcoi.app.endpoints.inventory.ListTypesEndpoint
import org.centrexcursionistalcoi.app.endpoints.inventory.UpdateItemEndpoint
import org.centrexcursionistalcoi.app.endpoints.inventory.UpdateTypesEndpoint
import org.centrexcursionistalcoi.app.endpoints.lending.AvailabilityEndpoint
import org.centrexcursionistalcoi.app.endpoints.lending.BookItemEndpoint
import org.centrexcursionistalcoi.app.endpoints.model.BasicAuthEndpoint
import org.centrexcursionistalcoi.app.endpoints.model.Endpoint
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.endpoints.sections.CreateSectionEndpoint
import org.centrexcursionistalcoi.app.endpoints.sections.ListSectionsEndpoint
import org.centrexcursionistalcoi.app.endpoints.sections.UpdateSectionEndpoint
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Routing")

private val endpoints: List<Endpoint> = listOf(
    RootEndpoint,
    LogoutEndpoint
)

private val basicAuthEndpoints: List<BasicAuthEndpoint> = listOf(
    RegisterEndpoint,
    LoginEndpoint
)

private val secureEndpoints: List<SecureEndpoint> = listOf(
    UserDataEndpoint,

    ListTypesEndpoint,
    CreateTypesEndpoint,
    UpdateTypesEndpoint,

    ListSectionsEndpoint,
    CreateSectionEndpoint,
    UpdateSectionEndpoint,

    ListItemsEndpoint,
    CreateItemEndpoint,
    UpdateItemEndpoint,

    BookItemEndpoint,
    AvailabilityEndpoint
)

private fun Route.configureEndpoint(endpoint: Endpoint) {
    logger.trace("${endpoint.method.value.padEnd(4, ' ')} :: ${endpoint.route}")
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
        authenticate(BASIC_AUTH_NAME) {
            logger.debug("Adding ${basicAuthEndpoints.size} basic auth endpoints...")
            for (endpoint in basicAuthEndpoints) {
                configureEndpoint(endpoint)
            }
        }
        logger.debug("Adding ${secureEndpoints.size} secure endpoints...")
        for (endpoint in secureEndpoints) {
            configureEndpoint(endpoint)
        }
    }
}
