package org.centrexcursionistalcoi.app.network

import io.github.vinceglb.filekit.PlatformFile
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItemType
import org.centrexcursionistalcoi.app.data.fileWithContext
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity.Companion.toEntity
import org.centrexcursionistalcoi.app.request.UpdateInventoryItemTypeRequest
import org.centrexcursionistalcoi.app.storage.InMemoryFileAllocator
import org.centrexcursionistalcoi.app.storage.SETTINGS_LAST_INVENTORY_ITEM_TYPES_SYNC
import org.centrexcursionistalcoi.app.utils.Zero
import org.koin.core.annotation.Singleton
import kotlin.uuid.Uuid

@Singleton
class InventoryItemTypesRemoteRepository(
    private val inventoryItemTypesRepository: InventoryItemTypesRepository,
) : RemoteRepository<Uuid, ReferencedInventoryItemType, Uuid, InventoryItemType>(
    "/inventory/types",
    SETTINGS_LAST_INVENTORY_ITEM_TYPES_SYNC,
    InventoryItemType.serializer(),
    inventoryItemTypesRepository,
    remoteToLocalIdConverter = { it },
) {
    suspend fun create(
        displayName: String,
        description: String?,
        categories: List<String>?,
        weight: Double?,
        department: Department?,
        image: PlatformFile?,
    ) {
        val imageUuid = image?.let { InMemoryFileAllocator.put(it) }

        create(InventoryItemType(Uuid.Zero, displayName, description, categories, weight, department?.id, imageUuid?.id))
    }

    suspend fun update(
        id: Uuid,
        displayName: String?,
        description: String?,
        categories: List<String>?,
        weight: Double?,
        department: Department?,
        image: PlatformFile?,
    ) {
        update(
            id,
            UpdateInventoryItemTypeRequest(displayName, description, categories, weight, department?.id, image?.fileWithContext()),
            UpdateInventoryItemTypeRequest.serializer(),
        )
    }

    override suspend fun insertRemoteEntity(entity: InventoryItemType): ReferencedInventoryItemType {
        inventoryItemTypesRepository.insert(entity.toEntity())
        return inventoryItemTypesRepository.get(entity.id)!!
    }

    override suspend fun updateRemoteEntity(entity: InventoryItemType): ReferencedInventoryItemType {
        inventoryItemTypesRepository.update(entity.toEntity())
        return inventoryItemTypesRepository.get(entity.id)!!
    }

    override suspend fun upsertRemoteEntity(entity: InventoryItemType): ReferencedInventoryItemType {
        inventoryItemTypesRepository.upsert(entity.toEntity())
        return inventoryItemTypesRepository.get(entity.id)!!
    }
}
