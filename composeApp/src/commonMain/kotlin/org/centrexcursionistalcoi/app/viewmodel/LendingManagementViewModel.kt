package org.centrexcursionistalcoi.app.viewmodel

import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.lending_details_scan_error_not_found
import com.diamondedge.logging.logging
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
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
import kotlin.uuid.Uuid

class LendingManagementViewModel(
    private val lendingId: Uuid,
): ErrorViewModel() {
    companion object {
        private val log = logging()
    }

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
                log.d { "NFC tag read: $payload" }
                payload.uuid()?.let { uuid ->
                    processScanById(uuid)
                }
                payload.id?.let { tagId ->
                    processScanByNfcId(tagId)
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
        launch {
            doAsync { processScanById(data.toUuidOrNull()) }
            doAsync { processScanByManufacturerTraceabilityCode(data) }
        }
    }

    fun confirmLending() = launch {
        try {
            doAsync {
                log.i { "Confirming lending..." }
                LendingsRemoteRepository.confirm(lendingId)
                log.i { "Lending has been confirmed." }
            }
        } catch (e: ServerException) {
            log.e(e) { "Error confirming lending" }
            setError(e)
        }
    }

    fun deleteLending() = launch {
        try {
            doAsync {
                log.i { "Deleting lending..." }
                LendingsRemoteRepository.delete(lendingId)
                log.i { "Lending has been deleted." }
            }
        } catch (e: ServerException) {
            log.e(e) { "Error deleting lending" }
            setError(e)
        }
    }

    fun skipMemory() = launch {
        try {
            doAsync {
                log.i { "Skipping memory for lending..." }
                LendingsRemoteRepository.skipMemory(lendingId)
                log.i { "Memory has been skipped for lending." }
            }
        } catch (e: ServerException) {
            log.e(e) { "Error skipping memory for lending" }
            setError(e)
        }
    }

    fun clearScanResult() {
        _scanError.value = null
        _scanSuccess.value = null
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

    private suspend fun processScan(predicate: (ReferencedInventoryItem) -> Boolean) {
        val lending = lending.value ?: return
        val item = lending.items.find(predicate)
        if (item == null) {
            log.e { "Could not find item matching predicate" }
            _scanError.value = getString(Res.string.lending_details_scan_error_not_found)
            return
        }

        _scannedItems.value += item.id
        _dismissedItems.value -= item.id
        log.i { "Item ${item.id} scanned successfully." }
    }

    private suspend fun processScanById(itemId: Uuid?) {
        itemId ?: return
        processScan { it.id == itemId }
    }

    private suspend fun processScanByManufacturerTraceabilityCode(manufacturerTraceabilityCode: String) {
        processScan { it.manufacturerTraceabilityCode == manufacturerTraceabilityCode }
    }

    private suspend fun processScanByNfcId(tagId: ByteArray) {
        processScan { it.nfcId.contentEquals(tagId) }
    }


    //
    // PICKUP LOGIC
    //

    fun pickup() = launch {
        try {
            doAsync {
                val dismissedItems = dismissedItems.value

                log.i { "Marking lending as picked up..." }
                log.d { "Dismissing ${dismissedItems.size} items: ${dismissedItems.joinToString()}" }
                LendingsRemoteRepository.pickup(lendingId, dismissItemsIds = dismissedItems.toList())
                log.i { "Lending has been marked as picked up." }
            }
        } catch (e: ServerException) {
            log.e(e) { "Error picking up lending" }
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
                    log.i { "Lending is not taken, skipping received items load." }
                    // No need to keep collecting
                    cancel()
                    return@collect
                }

                if (receivedItems == null) return@collect
                val lendingItems = lending.items.map { it.id }
                log.i { "Received items updated: ${receivedItems.size} / ${lendingItems.size} items received." }
                val receivedItemsIds = receivedItems.map { it.itemId }
                log.d { "Lending items:  ${lendingItems.joinToString()}" }
                log.d { "Received items: ${receivedItemsIds.joinToString()}" }
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
                log.i { "Returning lending..." }
                val scannedItems = scannedItems.value
                val notes = scannedItemsNotes.value.filterKeys { it in scannedItems }
                log.d { "Selected ${scannedItems.size} / ${lending.value?.items?.size} items" }
                LendingsRemoteRepository.`return`(
                    lendingId,
                    scannedItems.map { it to notes[it] }
                )
                log.i { "Return of lending has been received." }
            }
        } catch (e: ServerException) {
            log.e(e) { "Error returning lending" }
            setError(e)
        }
    }
}
