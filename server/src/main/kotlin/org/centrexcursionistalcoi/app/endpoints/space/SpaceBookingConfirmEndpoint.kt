package org.centrexcursionistalcoi.app.endpoints.space

import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.database.entity.SpaceBooking
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.endpoints.shared_logic.booking.confirmBooking

object SpaceBookingConfirmEndpoint: SecureEndpoint("/spaces/bookings/{id}/confirm") {
    override suspend fun RoutingContext.secureBody(user: User) {
        confirmBooking(user, SpaceBooking)
    }
}
