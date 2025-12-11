package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import com.mohamedrejeb.richeditor.model.RichTextState
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.Member
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.MembersRepository
import org.centrexcursionistalcoi.app.doAsync
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository
import org.centrexcursionistalcoi.app.process.Progress
import kotlin.uuid.Uuid

class ActivityMemoryEditorViewModel(private val lendingId: Uuid) : ViewModel() {

    /**
     * All active (non disabled) users.
     */
    val members = MembersRepository.selectAllAsFlow()
        // We also check for null, because non-admins only get provided active users, and their status is not given (it is always active/null).
        .map { members -> members.filter { it.status == null || it.status == Member.Status.ACTIVE } }
        .stateInViewModel()

    val departments = DepartmentsRepository.selectAllAsFlow().stateInViewModel()

    private val _isSaving = MutableStateFlow(false)
    val isSaving get() = _isSaving.asStateFlow()

    private val _saveProgress = MutableStateFlow<Progress?>(null)
    val saveProgress get() = _saveProgress.asStateFlow()

    private val _uploadSuccessful = MutableStateFlow(false)
    val uploadSuccessful get() = _uploadSuccessful.asStateFlow()

    fun save(place: String, members: List<Member>, externalUsers: String, sport: Sports?, department: Department?, text: RichTextState, files: List<PlatformFile>) = launch {
        try {
            _isSaving.value = true
            doAsync {
                val markdownText = text.toMarkdown()
                LendingsRemoteRepository.submitMemory(lendingId, place, members, externalUsers, sport, department, markdownText, files) { _saveProgress.value = it }
            }
            _uploadSuccessful.value = true
        } finally {
            _isSaving.value = false
        }
    }
}
