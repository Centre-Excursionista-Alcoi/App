package org.centrexcursionistalcoi.app.endpoints.space

import io.ktor.http.HttpMethod
import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.data.SpaceBookingD
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.SpaceBooking
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.database.table.SpaceBookingsTable
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.server.response.Errors

object SpaceBookingsListEndpoint: SecureEndpoint("/spaces/bookings", HttpMethod.Get) {
    override suspend fun RoutingContext.secureBody(user: User) {
        if (!user.confirmed) {
            respondFailure(Errors.UserNotConfirmed)
            return
        }

        val query = call.request.queryParameters
        val all = query["all"]?.toBoolean() ?: false

        val bookings = ServerDatabase {
            if (all && user.isAdmin) {
                SpaceBooking.all()
            } else {
                SpaceBooking.find { SpaceBookingsTable.user eq user.id }
            }.map(SpaceBooking::serializable)
        }
        respondSuccess(
            data = bookings,
            serializer = ListSerializer(SpaceBookingD.serializer())
        )
    }
}
