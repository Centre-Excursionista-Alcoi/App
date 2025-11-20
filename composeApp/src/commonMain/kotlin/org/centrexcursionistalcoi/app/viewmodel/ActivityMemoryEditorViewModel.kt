package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import com.mohamedrejeb.richeditor.model.RichTextState
import io.github.vinceglb.filekit.PlatformFile
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.database.UsersRepository
import org.centrexcursionistalcoi.app.doAsync
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository
import org.centrexcursionistalcoi.app.process.Progress

class ActivityMemoryEditorViewModel(private val lendingId: Uuid) : ViewModel() {

    /**
     * All active (non disabled) users.
     */
    val users = UsersRepository.selectAllAsFlow()
        .map { users -> users.filterNot { it.isDisabled }.sortedBy { it.fullName } }
        .stateInViewModel()

    private val _isSaving = MutableStateFlow(false)
    val isSaving get() = _isSaving.asStateFlow()

    private val _saveProgress = MutableStateFlow<Progress?>(null)
    val saveProgress get() = _saveProgress.asStateFlow()

    private val _uploadSuccessful = MutableStateFlow(false)
    val uploadSuccessful get() = _uploadSuccessful.asStateFlow()

    fun save(place: String, memberUsers: List<UserData>, externalUsers: String, sport: Sports?, text: RichTextState, files: List<PlatformFile>) = launch {
        try {
            _isSaving.value = true
            doAsync {
                val markdownText = text.toMarkdown()
                LendingsRemoteRepository.submitMemory(lendingId, place, memberUsers, externalUsers, sport, markdownText, files) { _saveProgress.value = it }
            }
            _uploadSuccessful.value = true
        } finally {
            _isSaving.value = false
        }
    }
}
