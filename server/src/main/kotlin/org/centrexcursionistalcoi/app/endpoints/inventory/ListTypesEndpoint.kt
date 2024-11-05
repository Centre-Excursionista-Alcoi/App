package org.centrexcursionistalcoi.app.endpoints.inventory

import io.ktor.http.HttpMethod
import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.ItemType
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.server.response.data.ItemTypeD

object ListTypesEndpoint: SecureEndpoint("/inventory/types", HttpMethod.Get) {
    override suspend fun RoutingContext.secureBody(user: User) {
        val items = ServerDatabase {
            ItemType.all().map(ItemType::serializable)
        }
        respondSuccess(
            items, ListSerializer(ItemTypeD.serializer())
        )
    }
}
