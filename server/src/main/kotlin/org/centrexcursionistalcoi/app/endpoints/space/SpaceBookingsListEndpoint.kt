package org.centrexcursionistalcoi.app.endpoints.space

import io.ktor.http.HttpMethod
import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.data.SpaceBookingD
import org.centrexcursionistalcoi.app.database.entity.SpaceBooking
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.database.table.SpaceBookingsTable
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.endpoints.shared_logic.booking.listBookings

object SpaceBookingsListEndpoint: SecureEndpoint("/spaces/bookings", HttpMethod.Get) {
    override suspend fun RoutingContext.secureBody(user: User) {
        listBookings(
            user = user,
            table = SpaceBookingsTable,
            entityClass = SpaceBooking,
            serializer = SpaceBookingD.serializer()
        )
    }
}
