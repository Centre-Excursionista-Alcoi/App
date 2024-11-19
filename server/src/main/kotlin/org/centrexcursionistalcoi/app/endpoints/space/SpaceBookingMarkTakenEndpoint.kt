package org.centrexcursionistalcoi.app.endpoints.space

import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.database.entity.SpaceBooking
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.endpoints.shared_logic.booking.markBookingAsTaken

object SpaceBookingMarkTakenEndpoint : SecureEndpoint("/spaces/bookings/{id}/taken") {
    override suspend fun RoutingContext.secureBody(user: User) {
        markBookingAsTaken(user, SpaceBooking)
    }
}
