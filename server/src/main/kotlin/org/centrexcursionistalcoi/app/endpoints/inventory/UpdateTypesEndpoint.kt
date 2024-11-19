package org.centrexcursionistalcoi.app.endpoints.inventory

import io.ktor.http.HttpMethod
import io.ktor.server.request.receive
import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.data.ItemTypeD
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.ItemType
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.server.response.Errors

object UpdateTypesEndpoint : SecureEndpoint("/inventory/types", HttpMethod.Patch) {
    override suspend fun RoutingContext.secureBody(user: User) {
        if (!user.isAdmin) {
            respondFailure(Errors.Forbidden)
            return
        }

        val body = call.receive<ItemTypeD>()
        val id = body.id
        if (id == null) {
            respondFailure(Errors.MissingId)
            return
        }
        val result = ServerDatabase {
            val item = ItemType.findById(id)
            if (item == null) {
                null
            } else {
                item.title = body.title
                item.description = body.description
                item.brand = body.brand
                item.model = body.model
            }
        }
        if (result == null) {
            respondFailure(Errors.ObjectNotFound)
            return
        }
        respondSuccess()
    }
}
