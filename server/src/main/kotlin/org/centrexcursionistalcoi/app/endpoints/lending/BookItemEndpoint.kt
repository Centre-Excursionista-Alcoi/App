package org.centrexcursionistalcoi.app.endpoints.lending

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.RoutingContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.centrexcursionistalcoi.app.data.ItemD
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.Item
import org.centrexcursionistalcoi.app.database.entity.Lending
import org.centrexcursionistalcoi.app.database.entity.LendingItem
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.database.utils.itemsAvailableForDates
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.server.request.LendingRequest
import org.centrexcursionistalcoi.app.server.response.Errors

object BookItemEndpoint : SecureEndpoint("/lending", HttpMethod.Post) {
    override suspend fun RoutingContext.secureBody(user: User) {
        if (!user.confirmed) {
            respondFailure(Errors.UserNotConfirmed)
            return
        }

        val body = call.receive<LendingRequest>()

        // Verify that the items exist
        val someItemDoesntExist = ServerDatabase { body.itemIds.any { Item.findById(it) == null } }
        if (someItemDoesntExist) {
            respondFailure(Errors.ObjectNotFound)
            return
        }

        // Verify that the item is available
        val from = Instant.ofEpochMilli(body.from)
            .let { LocalDateTime.ofInstant(it, ZoneId.systemDefault()) }
        val to = Instant.ofEpochMilli(body.to)
            .let { LocalDateTime.ofInstant(it, ZoneId.systemDefault()) }
        val requestedItemsIds = body.itemIds
        val availableItemsIds = ServerDatabase { itemsAvailableForDates(from, to) }.map(ItemD::id)
        val someItemNotAvailable = requestedItemsIds.any { it !in availableItemsIds }
        if (someItemNotAvailable) {
            respondFailure(Errors.InvalidRequest)
            return
        }

        // Create the lending
        ServerDatabase {
            val newLending = Lending.new {
                this.user = user
                this.from = from
                this.to = to
            }
            for (itemId in body.itemIds) {
                LendingItem.new {
                    lending = newLending
                    item = Item.findById(itemId)!!
                }
            }
        }

        respondSuccess(HttpStatusCode.Created)
    }
}
