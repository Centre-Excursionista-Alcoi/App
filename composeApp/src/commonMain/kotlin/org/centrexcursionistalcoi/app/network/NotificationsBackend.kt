package org.centrexcursionistalcoi.app.network

import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.data.NotificationD
import org.centrexcursionistalcoi.app.database.entity.Notification

object NotificationsBackend {
    suspend fun listNotifications() = Backend.get("/notifications", ListSerializer(NotificationD.serializer()))
        .map(Notification::deserialize)

    suspend fun markAsViewed(notification: Notification) = Backend.post("/notifications/${notification.id}/markAsViewed")
}
