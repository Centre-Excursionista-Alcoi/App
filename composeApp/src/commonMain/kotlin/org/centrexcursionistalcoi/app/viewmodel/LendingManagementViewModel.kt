package org.centrexcursionistalcoi.app.viewmodel

import cea_app.composeapp.generated.resources.*
import io.github.aakira.napier.Napier
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.database.LendingsRepository
import org.centrexcursionistalcoi.app.database.UsersRepository
import org.centrexcursionistalcoi.app.doAsync
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository
import org.centrexcursionistalcoi.app.platform.PlatformNFC
import org.centrexcursionistalcoi.app.platform.isNotSupported
import org.centrexcursionistalcoi.app.utils.toUuidOrNull
import org.jetbrains.compose.resources.getString
import org.ncgroup.kscan.Barcode

class LendingManagementViewModel(
    private val lendingId: Uuid,
): ErrorViewModel() {

    val lending = LendingsRepository.getAsFlow(lendingId).stateInViewModel()

    val toggleAllowIndeterminate = lending
        // Allow indeterminate for pickups
        .map { lending -> lending?.status() == Lending.Status.CONFIRMED }
        .stateInViewModel()

    val users = UsersRepository.selectAllAsFlow().stateInViewModel()

    private val _scannedItems = MutableStateFlow(emptySet<Uuid>())
    val scannedItems get() = _scannedItems.asStateFlow()

    private val _dismissedItems = MutableStateFlow(emptySet<Uuid>())
    val dismissedItems get() = _dismissedItems.asStateFlow()

    private val _scanError = MutableStateFlow<String?>(null)
    val scanError get() = _scanError.asStateFlow()

    private val _scanSuccess = MutableStateFlow<Unit?>(null)
    val scanSuccess get() = _scanSuccess.asStateFlow()

    private var nfcReaderJob: Job? = null

    fun startNfc() {
        if (PlatformNFC.isNotSupported) return

        nfcReaderJob = launch {
            while (true) {
                val payload = PlatformNFC.readNFC() ?: continue
                Napier.d("NFC tag read: $payload")
                payload.uuid()?.let { uuid ->
                    onScan(uuid)
                }
                payload.id?.let { tagId ->
                    processScan(tagId)
                }
            }
        }
    }

    fun stopNfc() {
        if (PlatformNFC.isNotSupported) return

        nfcReaderJob?.cancel()
        nfcReaderJob = null
    }

    private fun setScannedItems(items: Set<Uuid>) {
        _scannedItems.value = items
    }

    private fun setDismissedItems(items: Set<Uuid>) {
        _dismissedItems.value = items
    }

    fun onScan(barcode: Barcode) {
        val data = barcode.data
        val uuid = data.toUuidOrNull() ?: return
        launch {
            doAsync { processScan(uuid) }
        }
    }

    fun confirmLending() = launch {
        try {
            doAsync {
                Napier.i { "Confirming lending..." }
                LendingsRemoteRepository.confirm(lendingId)
                Napier.i { "Lending has been confirmed." }
            }
        } catch (e: ServerException) {
            Napier.e("Error confirming lending", e)
            setError(e)
        }
    }

    fun deleteLending() = launch {
        try {
            doAsync {
                Napier.i { "Deleting lending..." }
                LendingsRemoteRepository.delete(lendingId)
                Napier.i { "Lending has been deleted." }
            }
        } catch (e: ServerException) {
            Napier.e("Error deleting lending", e)
            setError(e)
        }
    }

    fun skipMemory() = launch {
        try {
            doAsync {
                Napier.i { "Skipping memory for lending..." }
                LendingsRemoteRepository.skipMemory(lendingId)
                Napier.i { "Memory has been skipped for lending." }
            }
        } catch (e: ServerException) {
            Napier.e("Error skipping memory for lending", e)
            setError(e)
        }
    }

    fun clearScanResult() {
        _scanError.value = null
        _scanSuccess.value = null
    }

    fun onScan(itemId: Uuid) = launch {
        doAsync {
            processScan(itemId)
        }
    }

    fun toggleItem(itemId: Uuid) {
        if (toggleAllowIndeterminate.value == true) {
            if (scannedItems.value.contains(itemId)) {
                // dismiss the item
                _dismissedItems.value += itemId
                _scannedItems.value -= itemId
            } else if (dismissedItems.value.contains(itemId)) {
                // clear the item status
                _dismissedItems.value -= itemId
            } else {
                // scan the item
                _scannedItems.value += itemId
            }
        } else {
            if (scannedItems.value.contains(itemId)) {
                // dismiss the item
                _dismissedItems.value += itemId
                _scannedItems.value -= itemId
            } else {
                // scan the item
                _scannedItems.value += itemId
                _dismissedItems.value -= itemId
            }
        }
    }

    private suspend fun processScan(itemId: Uuid) {
        val lending = lending.value ?: return
        val item = lending.items.find { it.id == itemId }
        if (item == null) {
            Napier.e { "Could not find item $itemId" }
            _scanError.value = getString(Res.string.lending_details_scan_error_not_found)
            return
        }

        _scannedItems.value += item.id
        _dismissedItems.value -= item.id
        Napier.i { "Item ${item.id} scanned successfully." }
    }

    private suspend fun processScan(tagId: ByteArray) {
        val lending = lending.value ?: return
        val item = lending.items.find { it.nfcId.contentEquals(tagId) }
        if (item == null) {
            Napier.e { "Could not find item for tag: ${tagId}" }
            _scanError.value = getString(Res.string.lending_details_scan_error_not_found)
            return
        }

        _scannedItems.value += item.id
        _dismissedItems.value -= item.id
        Napier.i { "Item ${item.id} scanned successfully." }
    }


    //
    // PICKUP LOGIC
    //

    fun pickup() = launch {
        try {
            doAsync {
                val dismissedItems = dismissedItems.value

                Napier.i { "Marking lending as picked up..." }
                Napier.d { "Dismissing ${dismissedItems.size} items: ${dismissedItems.joinToString()}" }
                LendingsRemoteRepository.pickup(lendingId, dismissItemsIds = dismissedItems.toList())
                Napier.i { "Lending has been marked as picked up." }
            }
        } catch (e: ServerException) {
            Napier.e("Error picking up lending", e)
            setError(e)
        }
    }


    //
    // RETURN LOGIC
    //

    val receivedItems = lending.map { it?.receivedItems }.stateInViewModel()

    private val _scannedItemsNotes = MutableStateFlow(emptyMap<Uuid, String>())
    val scannedItemsNotes get() = _scannedItemsNotes.asStateFlow()

    init {
        launch {
            // if it's a pickup (allow indeterminate) we do
            receivedItems.collect { receivedItems ->
                val lending = lending.value ?: return@collect
                if (lending.status() != Lending.Status.TAKEN) {
                    // Only load received items for taken lendings
                    Napier.i { "Lending is not taken, skipping received items load." }
                    // No need to keep collecting
                    cancel()
                    return@collect
                }

                if (receivedItems == null) return@collect
                val lendingItems = lending.items.map { it.id }
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

    fun `return`() = launch {
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
        } catch (e: ServerException) {
            Napier.e("Error returning lending", e)
            setError(e)
        }
    }
}
