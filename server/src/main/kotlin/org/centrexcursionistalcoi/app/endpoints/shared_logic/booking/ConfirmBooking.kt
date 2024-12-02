package org.centrexcursionistalcoi.app.endpoints.shared_logic.booking

import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.data.IBookingD
import org.centrexcursionistalcoi.app.data.enumeration.NotificationType
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.common.BookingEntity
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.database.entity.notification.Notification
import org.centrexcursionistalcoi.app.endpoints.space.SpaceBookingConfirmEndpoint.respondFailure
import org.centrexcursionistalcoi.app.endpoints.space.SpaceBookingConfirmEndpoint.respondSuccess
import org.centrexcursionistalcoi.app.push.FCM
import org.centrexcursionistalcoi.app.push.NotificationUtils.bookingType
import org.centrexcursionistalcoi.app.push.payload.BookingPayload
import org.centrexcursionistalcoi.app.server.response.Errors
import org.jetbrains.exposed.dao.IntEntityClass

suspend fun <Serializable : IBookingD, Entity : BookingEntity<Serializable>, EntityClass : IntEntityClass<Entity>> RoutingContext.confirmBooking(
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
    val bookingId = call.parameters["id"]?.toIntOrNull()
    val booking = bookingId?.let {
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

    val notification = ServerDatabase {
        Notification.new {
            this.type = NotificationType.BookingConfirmed
            this.payload = BookingPayload(
                bookingId = bookingId,
                bookingType = entityClass.bookingType()
            )
            this.userId = booking.user
        }
    }
    FCM.notify(notification)

    respondSuccess(HttpStatusCode.Accepted)
}
