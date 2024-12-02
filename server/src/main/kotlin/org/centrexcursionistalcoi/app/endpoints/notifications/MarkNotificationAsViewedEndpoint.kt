package org.centrexcursionistalcoi.app.endpoints.notifications

import io.ktor.http.HttpMethod
import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint

object MarkNotificationAsViewedEndpoint : SecureEndpoint("/notifications/{id}/markAsViewed", HttpMethod.Post) {
    override suspend fun RoutingContext.secureBody(user: User) {
        TODO("Not yet implemented")
    }
}
