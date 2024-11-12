package org.centrexcursionistalcoi.app.endpoints.inventory

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.Item
import org.centrexcursionistalcoi.app.database.entity.ItemType
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.server.response.Errors
import org.centrexcursionistalcoi.app.server.response.data.ItemD

object CreateItemEndpoint : SecureEndpoint("/inventory/items", HttpMethod.Post) {
    override suspend fun RoutingContext.secureBody(user: User) {
        if (!user.isAdmin) {
            respondFailure(Errors.Forbidden)
            return
        }

        val body = call.receive<ItemD>()
        val typeId = body.typeId
        if (typeId == null) {
            respondFailure(Errors.MissingReferenceId)
            return
        }
        val itemType = ServerDatabase { ItemType.findById(typeId) }
        if (itemType == null) {
            respondFailure(Errors.ReferenceNotFound)
            return
        }
        val itemAmount = body.amount
        if (itemAmount == null) {
            respondFailure(Errors.InvalidRequest)
            return
        }
        ServerDatabase {
            Item.new {
                health = body.health
                amount = itemAmount
                type = itemType
            }
        }
        respondSuccess(HttpStatusCode.Created)
    }
}
