package org.centrexcursionistalcoi.app.endpoints.lending

import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.database.entity.Lending
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.endpoints.shared_logic.booking.markBookingAsReturned

object MarkReturnedEndpoint : SecureEndpoint("/lending/{id}/returned") {
    override suspend fun RoutingContext.secureBody(user: User) {
        markBookingAsReturned(user, Lending)
    }
}
