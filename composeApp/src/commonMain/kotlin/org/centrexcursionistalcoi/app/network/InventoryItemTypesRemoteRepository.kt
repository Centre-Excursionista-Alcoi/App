package org.centrexcursionistalcoi.app.network

import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import org.centrexcursionistalcoi.app.request.UpdateInventoryItemTypeRequest
import org.centrexcursionistalcoi.app.storage.InMemoryFileAllocator
import org.centrexcursionistalcoi.app.utils.Zero

object InventoryItemTypesRemoteRepository : SymmetricRemoteRepository<Uuid, InventoryItemType>(
    "/inventory/types",
    InventoryItemType.serializer(),
    InventoryItemTypesRepository
) {
    suspend fun create(displayName: String, description: String?, image: ByteArray?) {
        val imageUuid = image?.let { InMemoryFileAllocator.put(it) }

        create(InventoryItemType(Uuid.Zero, displayName, description, imageUuid))
    }

    suspend fun update(id: Uuid, displayName: String?, description: String?, image: ByteArray?) {
        update(
            id,
            UpdateInventoryItemTypeRequest(displayName, description, image),
            UpdateInventoryItemTypeRequest.serializer(),
        )
    }
}
