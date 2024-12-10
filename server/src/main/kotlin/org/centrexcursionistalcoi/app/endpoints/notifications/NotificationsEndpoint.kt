package org.centrexcursionistalcoi.app.endpoints.notifications

import io.ktor.http.HttpMethod
import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.data.NotificationD
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.database.entity.notification.Notification
import org.centrexcursionistalcoi.app.database.table.NotificationsTable
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint

object NotificationsEndpoint : SecureEndpoint("/notifications", HttpMethod.Get) {
    override suspend fun RoutingContext.secureBody(user: User) {
        val notifications = ServerDatabase {
            Notification.find { NotificationsTable.userId eq user.id }
                .map(Notification::serializable)
        }
        respondSuccess(
            notifications, ListSerializer(NotificationD.serializer())
        )
    }
}
