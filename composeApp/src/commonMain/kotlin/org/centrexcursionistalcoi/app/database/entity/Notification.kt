package org.centrexcursionistalcoi.app.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import org.centrexcursionistalcoi.app.data.NotificationD
import org.centrexcursionistalcoi.app.data.enumeration.NotificationType
import org.centrexcursionistalcoi.app.push.payload.BookingPayload
import org.centrexcursionistalcoi.app.push.payload.PushPayload
import org.centrexcursionistalcoi.app.serverJson

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
}
