package org.centrexcursionistalcoi.app.endpoints.shared_logic.booking

import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.data.IBookingD
import org.centrexcursionistalcoi.app.data.enumeration.NotificationType
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.common.BookingEntity
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.database.entity.notification.Notification
import org.centrexcursionistalcoi.app.endpoints.space.SpaceBookingMarkReturnedEndpoint.respondFailure
import org.centrexcursionistalcoi.app.endpoints.space.SpaceBookingMarkReturnedEndpoint.respondSuccess
import org.centrexcursionistalcoi.app.push.FCM
import org.centrexcursionistalcoi.app.push.payload.BookingPayload
import org.centrexcursionistalcoi.app.server.response.Errors
import org.jetbrains.exposed.dao.IntEntityClass

suspend fun <Serializable : IBookingD, Entity : BookingEntity<Serializable>, EntityClass : IntEntityClass<Entity>> RoutingContext.deleteBooking(
    user: User,
    entityClass: EntityClass
) {
    // Verify that the booking is valid
    val bookingId = call.parameters["id"]?.toIntOrNull()
    val booking = bookingId?.let {
        ServerDatabase { entityClass.findById(it) }
    }
    if (booking == null) {
        respondFailure(Errors.ObjectNotFound)
        return
    }

    // Verify that the booking is not taken
    if (booking.takenAt != null) {
        respondFailure(Errors.BookingTaken)
        return
    }

    // Make sure the user is the owner of the booking, or is an admin
    val userId = user.id.value
    val bookingUserId = ServerDatabase { booking.user.id.value }
    if (bookingUserId != userId && !user.isAdmin) {
        respondFailure(Errors.Forbidden)
        return
    }

    // Mark the booking as taken
    ServerDatabase {
        booking.delete()
    }

    if (bookingUserId != userId) {
        val notification = ServerDatabase {
            Notification.new {
                this.type = NotificationType.BookingCancelled
                this.payload = BookingPayload(
                    bookingId = bookingId,
                    bookingType = entityClass::class.simpleName!!
                )
                this.userId = booking.user
            }
        }
        FCM.notify(notification)
    }

    respondSuccess()
}
