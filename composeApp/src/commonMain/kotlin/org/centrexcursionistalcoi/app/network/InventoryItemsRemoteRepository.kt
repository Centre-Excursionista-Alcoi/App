package org.centrexcursionistalcoi.app.network

import io.github.aakira.napier.Napier
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.http.isSuccess
import kotlin.uuid.Uuid
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
    suspend fun create(variation: String?, type: Uuid, nfcId: ByteArray?, progressNotifier: ProgressNotifier? = null) {
        create(InventoryItem(Uuid.Zero, variation, type, nfcId), progressNotifier)
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
        Napier.i("Created $amount inventory items of type $type with variation '$variation'")
        val correctCount = requests.count { it.status.isSuccess() }
        val failureCount = requests.size - correctCount
        Napier.d { "$correctCount were created successfully. There were $failureCount failures." }
        Napier.d { "Synchronizing completely with server..." }
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
}
