package org.centrexcursionistalcoi.app.endpoints.space

import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.SpaceBooking
import org.centrexcursionistalcoi.app.database.entity.SpaceKey
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.endpoints.shared_logic.booking.markBookingAsTaken
import org.centrexcursionistalcoi.app.server.response.Errors

object SpaceBookingMarkTakenEndpoint : SecureEndpoint("/spaces/bookings/{id}/taken/{keyId}") {
    override suspend fun RoutingContext.secureBody(user: User) {
        val keyId = call.parameters["keyId"]?.toIntOrNull()
        if (keyId == null) {
            respondFailure(Errors.InvalidRequest)
            return
        }
        val key = ServerDatabase { SpaceKey.findById(keyId) }
        if (key == null) {
            respondFailure(Errors.ObjectNotFound)
            return
        }

        markBookingAsTaken(user, SpaceBooking) { it.key = key }
    }
}
