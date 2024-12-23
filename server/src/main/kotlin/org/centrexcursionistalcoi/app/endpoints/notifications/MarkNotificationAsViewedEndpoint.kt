package org.centrexcursionistalcoi.app.endpoints.notifications

import io.ktor.http.HttpMethod
import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.database.entity.notification.Notification
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.server.response.Errors

object MarkNotificationAsViewedEndpoint : SecureEndpoint("/notifications/{id}/markAsViewed", HttpMethod.Post) {
    override suspend fun RoutingContext.secureBody(user: User) {
        val notificationId = call.parameters["id"]?.toIntOrNull()
        val notification = notificationId?.let { id ->
            ServerDatabase("MarkNotificationAsViewedEndpoint", "findById") { Notification.findById(id) }
        }
        if (notification == null) {
            respondFailure(Errors.ObjectNotFound)
            return
        }

        ServerDatabase("MarkNotificationAsViewedEndpoint", "markAsViewed") {
            notification.viewed = true
        }

        respondSuccess()
    }
}
