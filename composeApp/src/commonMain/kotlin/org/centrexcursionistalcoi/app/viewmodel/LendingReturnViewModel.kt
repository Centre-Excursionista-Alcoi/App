package org.centrexcursionistalcoi.app.viewmodel

import io.github.aakira.napier.Napier
import kotlin.uuid.Uuid
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.doAsync
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository

class LendingReturnViewModel(lendingId: Uuid): LendingPickupReturnViewModel(lendingId, toggleAllowIndeterminate = false) {
    val receivedItems = lending.map { it?.receivedItems }.stateInViewModel()

    private val _scannedItemsNotes = MutableStateFlow(emptyMap<Uuid, String>())
    val scannedItemsNotes get() = _scannedItemsNotes.asStateFlow()

    init {
        launch {
            receivedItems.collect { receivedItems ->
                if (receivedItems == null) return@collect
                val lendingItems = lending.value!!.items.map { it.id }
                Napier.i { "Received items updated: ${receivedItems.size} / ${lendingItems.size} items received." }
                val receivedItemsIds = receivedItems.map { it.itemId }
                Napier.d { "Lending items:  ${lendingItems.joinToString()}" }
                Napier.d { "Received items: ${receivedItemsIds.joinToString()}" }
                setScannedItems(receivedItemsIds.toSet())
                setDismissedItems(lendingItems.filter { it !in receivedItemsIds }.toSet())
                // Stop collecting once items have been loaded
                cancel()
            }
        }
    }

    fun `return`(onSuccess: () -> Unit) = launch {
        try {
            doAsync {
                Napier.i { "Returning lending..." }
                val scannedItems = scannedItems.value
                val notes = scannedItemsNotes.value.filterKeys { it in scannedItems }
                Napier.d { "Selected ${scannedItems.size} / ${lending.value?.items?.size} items" }
                LendingsRemoteRepository.`return`(
                    lendingId,
                    scannedItems.map { it to notes[it] }
                )
                Napier.i { "Return of lending has been received." }
            }
            onSuccess()
        } catch (e: ServerException) {
            Napier.e("Error returning lending", e)
            setError(e)
        }
    }
}
