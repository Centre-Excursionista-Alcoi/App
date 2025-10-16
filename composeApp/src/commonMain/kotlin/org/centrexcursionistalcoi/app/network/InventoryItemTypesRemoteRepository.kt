package org.centrexcursionistalcoi.app.network

import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import org.centrexcursionistalcoi.app.storage.InMemoryFileAllocator
import org.centrexcursionistalcoi.app.utils.Zero

object InventoryItemTypesRemoteRepository : RemoteRepository<Uuid, InventoryItemType>(
    "/inventory/types",
    InventoryItemType.serializer(),
    InventoryItemTypesRepository
) {
    suspend fun create(displayName: String, description: String?, image: ByteArray?) {
        val imageUuid = image?.let { InMemoryFileAllocator.put(it) }

        create(InventoryItemType(Uuid.Zero, displayName, description, imageUuid))
    }
}
