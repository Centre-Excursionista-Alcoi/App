package org.centrexcursionistalcoi.app.endpoints.space

import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.database.entity.SpaceBooking
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.endpoints.shared_logic.booking.markBookingAsReturned

object SpaceBookingMarkReturnedEndpoint : SecureEndpoint("/spaces/bookings/{id}/returned") {
    override suspend fun RoutingContext.secureBody(user: User) {
        markBookingAsReturned(user, SpaceBooking)
    }
}