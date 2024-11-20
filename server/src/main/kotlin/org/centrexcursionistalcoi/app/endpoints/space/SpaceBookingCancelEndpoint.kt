package org.centrexcursionistalcoi.app.endpoints.space

import io.ktor.http.HttpMethod
import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.database.entity.SpaceBooking
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.endpoints.shared_logic.booking.deleteBooking

object SpaceBookingCancelEndpoint : SecureEndpoint("/spaces/bookings/{id}", HttpMethod.Delete) {
    override suspend fun RoutingContext.secureBody(user: User) {
        deleteBooking(user, SpaceBooking)
    }
}
