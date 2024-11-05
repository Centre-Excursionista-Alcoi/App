package org.centrexcursionistalcoi.app.endpoints.inventory

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.ItemType
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.server.response.data.ItemTypeD

object CreateTypesEndpoint : SecureEndpoint("/inventory/types", HttpMethod.Post) {
    override suspend fun RoutingContext.secureBody(user: User) {
        val body = call.receive<ItemTypeD>()
        ServerDatabase {
            ItemType.new {
                title = body.title
                description = body.description
                brand = body.brand
                model = body.model
            }
        }
        respondSuccess(HttpStatusCode.Created)
    }
}
