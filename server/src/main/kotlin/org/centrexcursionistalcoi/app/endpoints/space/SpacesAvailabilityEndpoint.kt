package org.centrexcursionistalcoi.app.endpoints.space

import io.ktor.http.HttpMethod
import io.ktor.server.routing.RoutingContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.data.SpaceD
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.database.utils.spacesAvailableForDates
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.server.response.Errors

object SpacesAvailabilityEndpoint: SecureEndpoint("/spaces/availability", HttpMethod.Get) {
    override suspend fun RoutingContext.secureBody(user: User) {
        if (!user.confirmed) {
            respondFailure(Errors.UserNotConfirmed)
            return
        }

        val params = call.request.queryParameters
        val fromEpoch = params["from"]?.toLongOrNull()
        val toEpoch = params["to"]?.toLongOrNull()
        if (fromEpoch == null || toEpoch == null) {
            respondFailure(Errors.InvalidRequest)
            return
        }

        val from = Instant.ofEpochMilli(fromEpoch)
            .let { LocalDate.ofInstant(it, ZoneId.systemDefault()) }
        val to = Instant.ofEpochMilli(toEpoch)
            .let { LocalDate.ofInstant(it, ZoneId.systemDefault()) }

        // Fetch the existing bookings for the spaces that overlap with the requested period
        val availableSpaces = ServerDatabase { spacesAvailableForDates(from, to) }

        respondSuccess(
            data = availableSpaces,
            serializer = ListSerializer(SpaceD.serializer())
        )
    }
}
