package org.centrexcursionistalcoi.app.endpoints.shared_logic.booking

import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.KSerializer
import org.centrexcursionistalcoi.app.data.IBookingD
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.common.BookingEntity
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.space.SpaceBookingMarkReturnedEndpoint.respondFailure
import org.centrexcursionistalcoi.app.endpoints.space.SpaceBookingMarkReturnedEndpoint.respondSuccess
import org.centrexcursionistalcoi.app.server.response.Errors
import org.jetbrains.exposed.dao.IntEntityClass

suspend fun <Serializable : IBookingD, Entity : BookingEntity<Serializable>, EntityClass : IntEntityClass<Entity>> RoutingContext.getBooking(
    user: User,
    entityClass: EntityClass,
    serializer: KSerializer<Serializable>
) {
    // Verify that the booking is valid
    val booking = call.parameters["id"]
        ?.toIntOrNull()
        ?.let {
            ServerDatabase { entityClass.findById(it)?.serializable() }
        }
    if (booking == null) {
        respondFailure(Errors.ObjectNotFound)
        return
    }

    // Make sure the user is the owner of the booking, or is an admin
    if (booking.userId != user.id.value && !user.isAdmin) {
        respondFailure(Errors.Forbidden)
        return
    }

    respondSuccess(
        data = booking,
        serializer = serializer
    )
}
