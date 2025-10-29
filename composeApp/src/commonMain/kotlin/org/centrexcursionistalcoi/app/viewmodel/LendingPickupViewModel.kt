package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import cea_app.composeapp.generated.resources.*
import io.github.aakira.napier.Napier
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.centrexcursionistalcoi.app.database.LendingsRepository
import org.centrexcursionistalcoi.app.doAsync
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository
import org.centrexcursionistalcoi.app.platform.PlatformNFC
import org.centrexcursionistalcoi.app.platform.isNotSupported
import org.centrexcursionistalcoi.app.utils.toUuidOrNull
import org.jetbrains.compose.resources.getString
import org.ncgroup.kscan.Barcode

class LendingPickupViewModel(private val lendingId: Uuid): ViewModel() {

    val lending = LendingsRepository.getAsFlow(lendingId).stateInViewModel()

    private val _scannedItems = MutableStateFlow(emptySet<Uuid>())
    val scannedItems get() = _scannedItems.asStateFlow()

    private val _scanError = MutableStateFlow<String?>(null)
    val scanError get() = _scanError.asStateFlow()

    private val _scanSuccess = MutableStateFlow<Unit?>(null)
    val scanSuccess get() = _scanSuccess.asStateFlow()

    private var nfcReaderJob: Job? = null

    fun startNfc() {
        if (PlatformNFC.isNotSupported) return

        nfcReaderJob = launch {
            while (true) {
                val tag = PlatformNFC.readNFC() ?: continue
                Napier.d("NFC tag read: $tag")
                val uuid = tag.toUuidOrNull() ?: continue
                onScan(uuid)
            }
        }
    }

    fun stopNfc() {
        if (PlatformNFC.isNotSupported) return

        nfcReaderJob?.cancel()
        nfcReaderJob = null
    }

    fun onScan(barcode: Barcode) {
        val data = barcode.data
        val uuid = data.toUuidOrNull() ?: return
        launch {
            doAsync { processScan(uuid) }
        }
    }

    fun pickup() = launch {
        doAsync {
            Napier.i { "Marking lending as picked up..." }
            LendingsRemoteRepository.pickup(lendingId)
            Napier.i { "Lending has been marked as picked up." }
        }
    }

    fun cancelLending() = launch {
        doAsync {
            Napier.i { "Cancelling lending..." }
            LendingsRemoteRepository.cancel(lendingId)
            Napier.i { "Lending has been cancelled." }
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

    fun unmark(itemId: Uuid) {
        _scannedItems.value -= itemId
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
    }
}
