package org.centrexcursionistalcoi.app.database.entity.notification

import org.centrexcursionistalcoi.app.data.NotificationD
import org.centrexcursionistalcoi.app.database.common.SerializableEntity
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.database.table.NotificationsTable
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
            createdAt = createdAt.toEpochMilli(),
            viewed = viewed,
            type = type,
            payload = payload,
            userId = userId.id.value
        )
    }
}
