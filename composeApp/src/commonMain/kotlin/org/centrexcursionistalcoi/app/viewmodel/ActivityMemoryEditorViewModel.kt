package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.centrexcursionistalcoi.app.doAsync
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository

class ActivityMemoryEditorViewModel(private val lendingId: Uuid) : ViewModel() {
    private val _isSaving = MutableStateFlow(false)
    val isSaving get() = _isSaving.asStateFlow()

    private val _uploadSuccessful = MutableStateFlow(false)
    val uploadSuccessful get() = _uploadSuccessful.asStateFlow()

    fun save(state: RichTextState) = launch {
        try {
            _isSaving.value = true
            doAsync {
                val markdown = state.toMarkdown()
                LendingsRemoteRepository.submitMemory(lendingId, markdown)
            }
            _uploadSuccessful.value = true
        } finally {
            _isSaving.value = false
        }
    }
}
