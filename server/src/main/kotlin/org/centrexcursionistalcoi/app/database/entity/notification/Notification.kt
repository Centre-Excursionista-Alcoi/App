package org.centrexcursionistalcoi.app.database.entity.notification

import kotlinx.datetime.toKotlinInstant
import kotlinx.serialization.SerializationStrategy
import org.centrexcursionistalcoi.app.data.NotificationD
import org.centrexcursionistalcoi.app.data.enumeration.NotificationType
import org.centrexcursionistalcoi.app.database.common.SerializableEntity
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.database.table.NotificationsTable
import org.centrexcursionistalcoi.app.push.payload.BookingPayload
import org.centrexcursionistalcoi.app.push.payload.PushPayload
import org.centrexcursionistalcoi.app.serverJson
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Notification(id: EntityID<Int>) : SerializableEntity<NotificationD>(id) {
    companion object : IntEntityClass<Notification>(NotificationsTable)

    val createdAt by NotificationsTable.createdAt

    var viewed by NotificationsTable.viewed
    var type by NotificationsTable.type
    var payload by NotificationsTable.payload

    var userId by User referencedOn NotificationsTable.userId

    override fun serializable(): NotificationD {
        return NotificationD(
            id = id.value,
            createdAt = createdAt.toKotlinInstant(),
            viewed = viewed,
            type = type,
            payload = serverJson.encodeToString(
                if (type == NotificationType.BookingConfirmed || type == NotificationType.BookingCancelled) {
                    BookingPayload.serializer() as SerializationStrategy<PushPayload>
                } else {
                    throw UnsupportedOperationException("Unsupported notification type: $type")
                },
                payload
            ),
            userId = userId.id.value
        )
    }
}
