package org.centrexcursionistalcoi.app.endpoints.inventory

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.data.ItemD
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.Item
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.database.utils.findItemTypeById
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.server.response.Errors

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
        val itemType = findItemTypeById(typeId)
        if (itemType == null) {
            respondFailure(Errors.ReferenceNotFound)
            return
        }
        ServerDatabase("CreateItemEndpoint", "createItem") {
            Item.new {
                health = body.health
                notes = body.notes
                type = itemType
            }
        }
        respondSuccess(HttpStatusCode.Created)
    }
}
