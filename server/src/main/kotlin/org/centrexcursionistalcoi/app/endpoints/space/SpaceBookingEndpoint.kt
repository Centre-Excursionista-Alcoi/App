package org.centrexcursionistalcoi.app.endpoints.space

import io.ktor.http.HttpMethod
import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.data.SpaceBookingD
import org.centrexcursionistalcoi.app.database.entity.SpaceBooking
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.endpoints.shared_logic.booking.getBooking

object SpaceBookingEndpoint : SecureEndpoint("/spaces/bookings/{id}", HttpMethod.Get) {
    override suspend fun RoutingContext.secureBody(user: User) {
        getBooking(user, SpaceBooking, SpaceBookingD.serializer())
    }
}
