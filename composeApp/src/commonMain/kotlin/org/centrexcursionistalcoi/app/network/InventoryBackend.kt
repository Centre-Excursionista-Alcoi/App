package org.centrexcursionistalcoi.app.network

import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.server.response.data.ItemTypeD

object InventoryBackend {
    suspend fun listTypes() = Backend.get("/inventory/types", ListSerializer(ItemTypeD.serializer()))

    suspend fun create(itemType: ItemTypeD) = Backend.post(
        path = "/inventory/types",
        body = itemType,
        bodySerializer = ItemTypeD.serializer()
    )
}
