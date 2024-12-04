package org.centrexcursionistalcoi.app.database.entity

import androidx.compose.runtime.Composable
import androidx.room.Entity
import androidx.room.PrimaryKey
import ceaapp.composeapp.generated.resources.*
import kotlinx.datetime.Instant
import org.centrexcursionistalcoi.app.data.NotificationD
import org.centrexcursionistalcoi.app.data.enumeration.NotificationType
import org.centrexcursionistalcoi.app.push.payload.AdminBookingPayload
import org.centrexcursionistalcoi.app.push.payload.AdminNotificationType
import org.centrexcursionistalcoi.app.push.payload.AdminPayload
import org.centrexcursionistalcoi.app.push.payload.AdminUserPayload
import org.centrexcursionistalcoi.app.push.payload.BookingPayload
import org.centrexcursionistalcoi.app.push.payload.BookingType
import org.centrexcursionistalcoi.app.push.payload.PushPayload
import org.centrexcursionistalcoi.app.route.Home
import org.centrexcursionistalcoi.app.route.Reservation
import org.centrexcursionistalcoi.app.route.Route
import org.centrexcursionistalcoi.app.serverJson
import org.jetbrains.compose.resources.stringResource

@Entity
data class Notification(
    @PrimaryKey override val id: Int,
    override val createdAt: Instant,
    val viewed: Boolean,
    val type: NotificationType,
    val payload: String
) : DatabaseEntity<NotificationD> {
    companion object : EntityDeserializer<NotificationD, Notification> {
        override fun deserialize(source: NotificationD): Notification {
            return Notification(
                id = source.id,
                createdAt = source.createdAt,
                viewed = source.viewed,
                type = source.type,
                payload = source.payload
            )
        }

        private val notificationContents = mapOf(
            NotificationType.BookingConfirmed to (Res.string.notification_booking_confirmed_title to Res.string.notification_booking_confirmed_message),
            NotificationType.BookingCancelled to (Res.string.notification_booking_cancelled_title to Res.string.notification_booking_cancelled_message)
        )
    }

    override fun validate(): Boolean = true

    override fun serializable(): NotificationD {
        throw UnsupportedOperationException("Serialization not supported.")
    }

    /**
     * @return One of:
     * - [BookingPayload] for [NotificationType.BookingConfirmed] and [NotificationType.BookingCancelled]
     */
    fun payload(): PushPayload {
        return when (type) {
            NotificationType.BookingConfirmed -> serverJson.decodeFromString(BookingPayload.serializer(), payload)
            NotificationType.BookingCancelled -> serverJson.decodeFromString(BookingPayload.serializer(), payload)
            else -> throw UnsupportedOperationException("Unsupported notification type: $type")
        }
    }

    @Composable
    fun title(): String {
        return notificationContents[type]?.first?.let { stringResource(it) } ?: "Unsupported notification type: $type"
    }

    @Composable
    fun message(): String {
        return notificationContents[type]?.second?.let { stringResource(it) } ?: "Unsupported notification type: $type"
    }

    fun route(): Route {
        return when(
            val payload = payload()
        ) {
            is BookingPayload -> {
                when (payload.bookingType) {
                    BookingType.Lending -> {
                        Reservation(
                            lendingId = payload.bookingId,
                            spaceBookingId = null
                        )
                    }

                    BookingType.SpaceBooking -> {
                        Reservation(
                            lendingId = null,
                            spaceBookingId = payload.bookingId
                        )
                    }
                }
            }
            is AdminPayload -> when (payload) {
                is AdminBookingPayload -> when (payload.type) {
                    AdminNotificationType.NewBooking -> Home(showBookingIdString = payload.bookingIdString)
                    else -> throw UnsupportedOperationException("Unsupported admin notification type: ${payload.type}")
                }

                is AdminUserPayload -> when (payload.type) {
                    AdminNotificationType.NewUserRegistered -> Home(showUserId = payload.userId)
                    else -> throw UnsupportedOperationException("Unsupported admin notification type: ${payload.type}")
                }
            }
        }
    }
}
