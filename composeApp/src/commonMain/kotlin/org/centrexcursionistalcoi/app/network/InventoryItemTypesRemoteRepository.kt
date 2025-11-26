package org.centrexcursionistalcoi.app.network

import io.github.vinceglb.filekit.PlatformFile
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItemType
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItemType.Companion.referenced
import org.centrexcursionistalcoi.app.data.fileWithContext
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import org.centrexcursionistalcoi.app.request.UpdateInventoryItemTypeRequest
import org.centrexcursionistalcoi.app.storage.InMemoryFileAllocator
import org.centrexcursionistalcoi.app.storage.SETTINGS_LAST_INVENTORY_ITEM_TYPES_SYNC
import org.centrexcursionistalcoi.app.utils.Zero

object InventoryItemTypesRemoteRepository : RemoteRepository<Uuid, ReferencedInventoryItemType, Uuid, InventoryItemType>(
    "/inventory/types",
    SETTINGS_LAST_INVENTORY_ITEM_TYPES_SYNC,
    InventoryItemType.serializer(),
    InventoryItemTypesRepository,
    remoteToLocalIdConverter = { it },
    remoteToLocalEntityConverter = { inventoryItemType ->
        val departments = DepartmentsRepository.selectAll()
        inventoryItemType.referenced(departments)
    },
) {
    suspend fun create(
        displayName: String,
        description: String?,
        categories: List<String>?,
        department: Department?,
        image: PlatformFile?,
    ) {
        val imageUuid = image?.let { InMemoryFileAllocator.put(it) }

        create(InventoryItemType(Uuid.Zero, displayName, description, categories, department?.id, imageUuid?.id))
    }

    suspend fun update(
        id: Uuid,
        displayName: String?,
        description: String?,
        categories: List<String>?,
        department: Department?,
        image: PlatformFile?,
    ) {
        update(
            id,
            UpdateInventoryItemTypeRequest(displayName, description, categories, department?.id, image?.fileWithContext()),
            UpdateInventoryItemTypeRequest.serializer(),
        )
    }
}
