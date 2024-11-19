package org.centrexcursionistalcoi.app.endpoints.inventory

import io.ktor.http.HttpMethod
import io.ktor.server.routing.RoutingContext
import org.centrexcursionistalcoi.app.data.ItemTypeD
import org.centrexcursionistalcoi.app.database.entity.ItemType
import org.centrexcursionistalcoi.app.database.entity.User
import org.centrexcursionistalcoi.app.endpoints.model.SecureEndpoint
import org.centrexcursionistalcoi.app.endpoints.shared_logic.list.listAllDatabaseEntries

object ListTypesEndpoint: SecureEndpoint("/inventory/types", HttpMethod.Get) {
    override suspend fun RoutingContext.secureBody(user: User) {
        listAllDatabaseEntries(user, ItemType, ItemTypeD.serializer())
    }
}
