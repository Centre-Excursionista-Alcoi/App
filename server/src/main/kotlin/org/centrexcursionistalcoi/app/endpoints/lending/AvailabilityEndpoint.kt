package org.centrexcursionistalcoi.app.endpoints.lending

import io.ktor.http.HttpMethod
import io.ktor.server.routing.RoutingContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.Item
import org.centrexcursionistalcoi.app.database.entity.Lending
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.database.table.ItemsTable
import org.centrexcursionistalcoi.app.database.table.LendingsTable
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.server.response.Errors
import org.centrexcursionistalcoi.app.server.response.data.ItemD
import org.jetbrains.exposed.sql.and

object AvailabilityEndpoint: SecureEndpoint("/availability", HttpMethod.Get) {
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
            .let { LocalDateTime.ofInstant(it, ZoneId.systemDefault()) }
        val to = Instant.ofEpochMilli(toEpoch)
            .let { LocalDateTime.ofInstant(it, ZoneId.systemDefault()) }

        // Fetch the existing lendings for the item that overlap with the requested period
        val lendings = ServerDatabase {
            Lending.find {
                (LendingsTable.from less from) and (LendingsTable.to greater to)
            }.map(Lending::serializable)
        }
        val usedItemIds = lendings.mapNotNull { it.itemId }
        // Fetch all the items that are not lent during the requested period
        val availableItems = ServerDatabase {
            Item.find { ItemsTable.id notInList  usedItemIds }
                .map(Item::serializable)
        }

        respondSuccess(
            data = availableItems,
            serializer = ListSerializer(ItemD.serializer())
        )
    }
}
