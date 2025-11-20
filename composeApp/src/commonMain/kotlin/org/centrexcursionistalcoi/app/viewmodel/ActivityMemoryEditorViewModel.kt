package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.database.UsersRepository
import org.centrexcursionistalcoi.app.doAsync
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository

class ActivityMemoryEditorViewModel(private val lendingId: Uuid) : ViewModel() {

    /**
     * All active (non disabled) users.
     */
    val users = UsersRepository.selectAllAsFlow()
        .map { users -> users.filterNot { it.isDisabled }.sortedBy { it.fullName } }
        .stateInViewModel()

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
