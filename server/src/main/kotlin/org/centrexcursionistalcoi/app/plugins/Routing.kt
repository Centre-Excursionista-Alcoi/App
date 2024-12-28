package org.centrexcursionistalcoi.app.plugins

import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.*
import org.centrexcursionistalcoi.app.endpoints.RootEndpoint
import org.centrexcursionistalcoi.app.endpoints.auth.*
import org.centrexcursionistalcoi.app.endpoints.inventory.*
import org.centrexcursionistalcoi.app.endpoints.lending.*
import org.centrexcursionistalcoi.app.endpoints.model.BasicAuthEndpoint
import org.centrexcursionistalcoi.app.endpoints.model.Endpoint
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.endpoints.notifications.MarkNotificationAsViewedEndpoint
import org.centrexcursionistalcoi.app.endpoints.notifications.NotificationsEndpoint
import org.centrexcursionistalcoi.app.endpoints.sections.CreateSectionEndpoint
import org.centrexcursionistalcoi.app.endpoints.sections.ListSectionsEndpoint
import org.centrexcursionistalcoi.app.endpoints.sections.UpdateSectionEndpoint
import org.centrexcursionistalcoi.app.endpoints.space.*
import org.centrexcursionistalcoi.app.endpoints.status.PingEndpoint
import org.centrexcursionistalcoi.app.endpoints.status.VersionEndpoint
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Routing")

private val endpoints: List<Endpoint> = listOf(
    RootEndpoint,
    LogoutEndpoint,
    PingEndpoint,
    VersionEndpoint
)

private val basicAuthEndpoints: List<BasicAuthEndpoint> = listOf(
    RegisterEndpoint,
    LoginEndpoint
)

private val secureEndpoints: List<SecureEndpoint> = listOf(
    UserDataEndpoint,
    UsersEndpoint,
    ConfirmUserEndpoint,
    DeleteUserEndpoint,
    UpdateFCMTokenEndpoint,

    NotificationsEndpoint,
    MarkNotificationAsViewedEndpoint,

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
    LendingEndpoint,
    AvailabilityEndpoint,
    LendingsEndpoint,
    ConfirmEndpoint,
    MarkTakenEndpoint,
    MarkReturnedEndpoint,
    CancelLendingEndpoint,

    SpaceEndpoint,
    SpacesListEndpoint,
    SpaceCreateEndpoint,
    SpaceUpdateEndpoint,
    SpacesAvailabilityEndpoint,
    SpaceBookEndpoint,
    SpaceBookingEndpoint,
    SpaceBookingsListEndpoint,
    SpaceBookingCancelEndpoint,
    SpaceBookingConfirmEndpoint,
    SpaceBookingMarkTakenEndpoint,
    SpaceBookingMarkTakenKeyEndpoint,
    SpaceBookingMarkReturnedEndpoint
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
