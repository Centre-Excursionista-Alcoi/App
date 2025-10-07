package org.centrexcursionistalcoi.app.network

import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.data.InventoryItem
import org.centrexcursionistalcoi.app.database.InventoryItemsRepository
import org.centrexcursionistalcoi.app.utils.Zero

object InventoryItemsRemoteRepository : RemoteRepository<Uuid, InventoryItem>(
    "/inventory/items",
    InventoryItem.serializer(),
    InventoryItemsRepository
) {
    suspend fun create(variation: String?, type: Uuid) {
        create(InventoryItem(Uuid.Zero, variation, type))
    }
}
