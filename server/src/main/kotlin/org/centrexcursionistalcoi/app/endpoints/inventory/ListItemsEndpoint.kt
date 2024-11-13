package org.centrexcursionistalcoi.app.endpoints.inventory

import io.ktor.http.HttpMethod
import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.database.ServerDatabase
import org.centrexcursionistalcoi.app.database.entity.Item
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.database.table.ItemsTable
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.server.response.data.ItemD

object ListItemsEndpoint: SecureEndpoint("/inventory/items", HttpMethod.Get) {
    override suspend fun RoutingContext.secureBody(user: User) {
        val query = call.request.queryParameters
        val filterItems = query["filterItems"]?.split(',')?.mapNotNull { it.toIntOrNull() } ?: emptyList()

        val items = ServerDatabase {
            if (filterItems.isEmpty()) {
                Item.all()
            } else {
                Item.find { ItemsTable.id inList filterItems }
            }.map(Item::serializable)
        }
        respondSuccess(
            items, ListSerializer(ItemD.serializer())
        )
    }
}
