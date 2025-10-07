package org.centrexcursionistalcoi.app.network

import io.github.aakira.napier.Napier
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.http.isSuccess
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

    suspend fun create(variation: String?, type: Uuid, amount: Int) {
        val requests = (0 until amount).map {
            httpClient.submitFormWithBinaryData(
                url = endpoint,
                formData = formData {
                    append("type", type.toString())
                    variation?.let { append("variation", it) }
                }
            )
        }
        Napier.i("Created $amount inventory items of type $type with variation '$variation'")
        val correctCount = requests.count { it.status.isSuccess() }
        val failureCount = requests.size - correctCount
        Napier.d { "$correctCount were created successfully. There were $failureCount failures." }
        Napier.d { "Synchronizing completely with server..." }
        synchronizeWithDatabase()
    }
}
