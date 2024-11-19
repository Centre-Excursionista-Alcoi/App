package org.centrexcursionistalcoi.app.endpoints.shared_logic.booking

import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.common.BookingEntity
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.space.SpaceBookingConfirmEndpoint.respondFailure
import org.centrexcursionistalcoi.app.endpoints.space.SpaceBookingConfirmEndpoint.respondSuccess
import org.centrexcursionistalcoi.app.server.response.Errors
import org.jetbrains.exposed.dao.IntEntityClass

suspend fun <Entity: BookingEntity, EntityClass: IntEntityClass<Entity>> RoutingContext.confirmBooking(
    user: User,
    entityClass: EntityClass
) {
    if (!user.confirmed) {
        respondFailure(Errors.UserNotConfirmed)
        return
    }

    // Verify that the user is admin
    if (!user.isAdmin) {
        respondFailure(Errors.Forbidden)
        return
    }

    // Verify that the booking is valid
    val booking = call.parameters["id"]
        ?.toIntOrNull()
        ?.let {
            ServerDatabase { entityClass.findById(it) }
        }
    if (booking == null) {
        respondFailure(Errors.ObjectNotFound)
        return
    }

    // Confirm the booking
    ServerDatabase {
        booking.confirmed = true
    }

    respondSuccess(HttpStatusCode.Accepted)
}
