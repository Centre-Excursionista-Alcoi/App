package org.centrexcursionistalcoi.app.endpoints.lending

import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.RoutingContext
import java.time.Instant
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.Lending
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.server.response.Errors

object MarkTakenEndpoint : SecureEndpoint("/lending/{bookingId}/taken") {
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

        // Verify that the booking is confirmed
        if (!booking.confirmed) {
            respondFailure(Errors.InvalidRequest)
            return
        }

        // Verify that the booking is not already taken
        if (booking.takenAt != null) {
            respondFailure(Errors.InvalidRequest)
            return
        }

        // Mark the booking as taken
        ServerDatabase {
            booking.takenAt = Instant.now()
        }

        respondSuccess(HttpStatusCode.Accepted)
    }
}
