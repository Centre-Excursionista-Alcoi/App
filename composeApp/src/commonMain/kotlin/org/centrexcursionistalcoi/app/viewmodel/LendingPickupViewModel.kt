package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.database.LendingsRepository
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository
import org.centrexcursionistalcoi.app.platform.PlatformNFC
import org.centrexcursionistalcoi.app.utils.toUuidOrNull
import org.ncgroup.kscan.Barcode

class LendingPickupViewModel(lendingId: Uuid): ViewModel() {

    val lending = LendingsRepository.getAsFlow(lendingId).stateInViewModel()

    private val _scannedItems = MutableStateFlow(emptySet<Uuid>())
    val scannedItems get() = _scannedItems.asStateFlow()

    private var nfcReaderJob: Job? = null

    fun startNfc() {
        if (!PlatformNFC.supportsNFC) return

        nfcReaderJob = viewModelScope.launch {
            while (true) {
                val tag = PlatformNFC.readNFC() ?: continue
                Napier.d("NFC tag read: $tag")
                val uuid = tag.toUuidOrNull() ?: continue
                onScan(uuid)
            }
        }
    }

    fun stopNfc() {
        if (!PlatformNFC.supportsNFC) return

        nfcReaderJob?.cancel()
        nfcReaderJob = null
    }

    fun onScan(barcode: Barcode) {
        val data = barcode.data
        val uuid = data.toUuidOrNull() ?: return
        onScan(uuid)
    }

    fun pickup(lending: ReferencedLending) {
        viewModelScope.launch(defaultAsyncDispatcher) {
            Napier.i { "Marking lending as picked up..." }
            LendingsRemoteRepository.pickup(lending.id)
            Napier.i { "Lending has been marked as picked up." }
        }
    }

    fun onScan(itemId: Uuid) {
        // TODO: Implement item scan logic
    }
}
