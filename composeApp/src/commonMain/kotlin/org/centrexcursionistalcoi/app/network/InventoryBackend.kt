package org.centrexcursionistalcoi.app.network

import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.server.response.data.ItemD
import org.centrexcursionistalcoi.app.server.response.data.ItemTypeD

object InventoryBackend {
    suspend fun listTypes() = Backend.get("/inventory/types", ListSerializer(ItemTypeD.serializer()))

    suspend fun create(itemType: ItemTypeD) = Backend.post(
        path = "/inventory/types",
        body = itemType,
        bodySerializer = ItemTypeD.serializer()
    )

    suspend fun update(itemType: ItemTypeD) = Backend.patch(
        path = "/inventory/types",
        body = itemType,
        bodySerializer = ItemTypeD.serializer()
    )

    suspend fun listItems() = Backend.get("/inventory/items", ListSerializer(ItemD.serializer()))

    suspend fun create(item: ItemD) = Backend.post(
        path = "/inventory/items",
        body = item,
        bodySerializer = ItemD.serializer()
    )

    suspend fun update(item: ItemD) = Backend.patch(
        path = "/inventory/items",
        body = item,
        bodySerializer = ItemD.serializer()
    )
}
