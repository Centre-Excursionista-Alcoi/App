package org.centrexcursionistalcoi.app.endpoints.inventory

import io.ktor.http.HttpMethod
import io.ktor.server.request.receive
import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.data.ItemD
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.Item
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.server.response.Errors

object UpdateItemEndpoint : SecureEndpoint("/inventory/items", HttpMethod.Patch) {
    override suspend fun RoutingContext.secureBody(user: User) {
        if (!user.isAdmin) {
            respondFailure(Errors.Forbidden)
            return
        }

        val body = call.receive<ItemD>()
        val id = body.id
        if (id == null) {
            respondFailure(Errors.MissingId)
            return
        }
        val result = ServerDatabase("UpdateItemEndpoint", "findItemById") {
            val item = Item.findById(id)
            if (item == null) {
                null
            } else {
                item.health = body.health
            }
        }
        if (result == null) {
            respondFailure(Errors.ObjectNotFound)
            return
        }
        respondSuccess()
    }
}
