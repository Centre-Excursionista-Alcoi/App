package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import io.github.vinceglb.filekit.PlatformFile
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.centrexcursionistalcoi.app.database.LendingsRepository
import org.centrexcursionistalcoi.app.doAsync
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository
import org.centrexcursionistalcoi.app.process.Progress

class LendingDetailsModel(private val lendingId: Uuid): ViewModel() {
    val lending = LendingsRepository.getAsFlow(lendingId).stateInViewModel()

    private val _memoryUploadProgress = MutableStateFlow<Progress?>(null)
    val memoryUploadProgress = _memoryUploadProgress.asStateFlow()

    fun cancelLending() = async {
        try {
            doAsync { LendingsRemoteRepository.cancel(lendingId) }
            null
        } catch (error: ServerException) {
            error
        }
    }

    fun submitMemory(file: PlatformFile) = async {
        try {
            LendingsRemoteRepository.submitMemory(lendingId, file) { _memoryUploadProgress.value = it }
            null
        } catch (error: ServerException) {
            error
        }
    }
}
