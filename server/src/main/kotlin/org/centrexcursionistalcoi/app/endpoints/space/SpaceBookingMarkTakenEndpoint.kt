package org.centrexcursionistalcoi.app.endpoints.space

import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.SpaceBooking
import org.centrexcursionistalcoi.app.database.entity.SpaceKey
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.database.table.SpaceKeysTable
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.endpoints.shared_logic.booking.markBookingAsTaken
import org.centrexcursionistalcoi.app.server.response.Errors

object SpaceBookingMarkTakenEndpoint : SecureEndpoint("/spaces/bookings/{id}/taken") {
    override suspend fun RoutingContext.secureBody(user: User) {
        markBookingAsTaken(
            user,
            SpaceBooking,
            extraBookingValidations = { booking ->
                val spaceKeysCount = ServerDatabase {
                    SpaceKey.find { SpaceKeysTable.space eq booking.space.id }.count()
                }
                if (spaceKeysCount <= 0) {
                    true
                } else {
                    respondFailure(Errors.KeyNotSpecified)
                    false
                }
            }
        )
    }
}
