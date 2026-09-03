package org.centrexcursionistalcoi.app.network

import com.diamondedge.logging.logging
import org.centrexcursionistalcoi.app.data.InventoryItem
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem.Companion.referenced
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import org.centrexcursionistalcoi.app.database.InventoryItemsRepository
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.request.UpdateInventoryItemRequest
import org.centrexcursionistalcoi.app.storage.SETTINGS_LAST_INVENTORY_ITEMS_SYNC
import org.centrexcursionistalcoi.app.utils.Zero
import org.koin.core.annotation.Singleton
import kotlin.uuid.Uuid

@Singleton
class InventoryItemsRemoteRepository(
    inventoryItemsRepository: InventoryItemsRepository,
    inventoryItemTypesRepository: InventoryItemTypesRepository
) : RemoteRepository<Uuid, ReferencedInventoryItem, Uuid, InventoryItem>(
    "/inventory/items",
    SETTINGS_LAST_INVENTORY_ITEMS_SYNC,
    InventoryItem.serializer(),
    inventoryItemsRepository,
    remoteToLocalIdConverter = { it },
    remoteToLocalEntityConverter = { item ->
        val type = inventoryItemTypesRepository.get(item.type) ?: throw NoSuchElementException("No inventory item type with ID ${item.type} found for inventory item ${item.id}")
        item.referenced(type)
    },
) {
    private val log = logging()

    suspend fun create(variation: String?, type: Uuid, nfcId: ByteArray?, manufacturerTraceabilityCode: String?, progressNotifier: ProgressNotifier? = null) {
        create(InventoryItem(Uuid.Zero, variation, type, nfcId, manufacturerTraceabilityCode), progressNotifier)
    }

    suspend fun create(variation: String?, type: Uuid, amount: Int, progressNotifier: ProgressNotifier? = null) {
        val requests = (0 until amount).map {
            try {
                create(InventoryItem(Uuid.Zero, variation, type, null, null), progressNotifier)
                true
            } catch (e: Exception) {
                log.e(e) { "Failed to create inventory item of type $type with variation '$variation'" }
                false
            }
        }
        log.i { "Created $amount inventory items of type $type with variation '$variation'" }
        val correctCount = requests.count { it }
        val failureCount = requests.size - correctCount
        log.d { "$correctCount were created successfully. There were $failureCount failures." }
    }

    suspend fun update(id: Uuid, variation: String?, nfcId: ByteArray?, progressNotifier: ProgressNotifier? = null) {
        update(
            id,
            UpdateInventoryItemRequest(variation, nfcId = nfcId),
            UpdateInventoryItemRequest.serializer(),
            progressNotifier
        )
    }

    suspend fun updateManufacturerData(id: Uuid, data: String, progressNotifier: ProgressNotifier? = null) {
        update(
            id,
            UpdateInventoryItemRequest(manufacturerTraceabilityCode = data),
            UpdateInventoryItemRequest.serializer(),
            progressNotifier
        )
    }
}
