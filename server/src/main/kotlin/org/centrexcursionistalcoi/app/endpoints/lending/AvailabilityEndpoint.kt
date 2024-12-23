package org.centrexcursionistalcoi.app.endpoints.lending

import io.ktor.http.HttpMethod
import io.ktor.server.routing.RoutingContext
import java.time.LocalDate
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.data.ItemD
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.database.utils.itemsAvailableForDates
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.server.response.Errors

object AvailabilityEndpoint: SecureEndpoint("/availability", HttpMethod.Get) {
    override suspend fun RoutingContext.secureBody(user: User) {
        if (!user.confirmed) {
            respondFailure(Errors.UserNotConfirmed)
            return
        }

        val params = call.request.queryParameters
        val from = params["from"]?.let(LocalDate::parse)
        val to = params["to"]?.let(LocalDate::parse)
        if (from == null || to == null) {
            respondFailure(Errors.InvalidRequest)
            return
        }

        // Fetch the existing lendings for the item that overlap with the requested period
        val availableItems = ServerDatabase("AvailabilityEndpoint", "itemsAvailableForDates") {
            itemsAvailableForDates(from, to)
        }

        respondSuccess(
            data = availableItems,
            serializer = ListSerializer(ItemD.serializer())
        )
    }
}
