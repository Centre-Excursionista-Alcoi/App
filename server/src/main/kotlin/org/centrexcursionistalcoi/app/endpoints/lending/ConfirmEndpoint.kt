package org.centrexcursionistalcoi.app.endpoints.lending

import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.Lending
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.server.response.Errors

object ConfirmEndpoint : SecureEndpoint("/lending/{bookingId}/confirm") {
    override suspend fun RoutingContext.secureBody(user: User) {
        // Verify that the user is admin
        if (!user.isAdmin) {
            respondFailure(Errors.Forbidden)
            return
        }

        // Verify that the booking is valid
        val booking = call.parameters["bookingId"]
            ?.toIntOrNull()
            ?.let {
                ServerDatabase { Lending.findById(it) }
            }
        if (booking == null) {
            respondFailure(Errors.InvalidRequest)
            return
        }

        // Confirm the booking
        ServerDatabase {
            booking.confirmed = true
        }

        respondSuccess(HttpStatusCode.Accepted)
    }
}
