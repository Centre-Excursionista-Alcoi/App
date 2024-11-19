package org.centrexcursionistalcoi.app.endpoints.lending

import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.database.entity.Lending
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.endpoints.shared_logic.booking.confirmBooking

object ConfirmEndpoint : SecureEndpoint("/lending/{id}/confirm") {
    override suspend fun RoutingContext.secureBody(user: User) {
        confirmBooking(user, Lending)
    }
}
