package org.centrexcursionistalcoi.app.network

import com.diamondedge.logging.logging
import io.ktor.client.request.forms.*
import io.ktor.http.*
import org.centrexcursionistalcoi.app.data.InventoryItem
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem.Companion.referenced
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import org.centrexcursionistalcoi.app.database.InventoryItemsRepository
import org.centrexcursionistalcoi.app.process.Progress.Companion.monitorUploadProgress
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.request.UpdateInventoryItemRequest
import org.centrexcursionistalcoi.app.storage.SETTINGS_LAST_INVENTORY_ITEMS_SYNC
import org.centrexcursionistalcoi.app.utils.Zero
import kotlin.uuid.Uuid

object InventoryItemsRemoteRepository : RemoteRepository<Uuid, ReferencedInventoryItem, Uuid, InventoryItem>(
    "/inventory/items",
    SETTINGS_LAST_INVENTORY_ITEMS_SYNC,
    InventoryItem.serializer(),
    InventoryItemsRepository,
    remoteToLocalIdConverter = { it },
    remoteToLocalEntityConverter = { item ->
        val type = InventoryItemTypesRepository.get(item.type) ?: throw NoSuchElementException("No inventory item type with ID ${item.type} found for inventory item ${item.id}")
        item.referenced(type)
    },
) {
    private val log = logging()

    suspend fun create(variation: String?, type: Uuid, nfcId: ByteArray?, manufacturerTraceabilityCode: String?, progressNotifier: ProgressNotifier? = null) {
        create(InventoryItem(Uuid.Zero, variation, type, nfcId, manufacturerTraceabilityCode), progressNotifier)
    }

    suspend fun create(variation: String?, type: Uuid, amount: Int, progressNotifier: ProgressNotifier? = null) {
        val requests = (0 until amount).map {
            httpClient.submitFormWithBinaryData(
                url = endpoint,
                formData = formData {
                    append("type", type.toString())
                    variation?.let { append("variation", it) }
                }
            ) {
                progressNotifier?.let { monitorUploadProgress(it) }
            }
        }
        log.i { "Created $amount inventory items of type $type with variation '$variation'" }
        val correctCount = requests.count { it.status.isSuccess() }
        val failureCount = requests.size - correctCount
        log.d { "$correctCount were created successfully. There were $failureCount failures." }
        log.d { "Synchronizing completely with server..." }
        synchronizeWithDatabase(progressNotifier)
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
