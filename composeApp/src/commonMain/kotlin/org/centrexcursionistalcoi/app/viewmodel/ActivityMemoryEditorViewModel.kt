package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import com.mohamedrejeb.richeditor.model.RichTextState
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.Member
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.data.ZonedDateTime
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.MembersRepository
import org.centrexcursionistalcoi.app.database.MemoriesRepository
import org.centrexcursionistalcoi.app.di.DispatcherProvider
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository
import org.centrexcursionistalcoi.app.network.MemoriesRemoteRepository
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import kotlin.uuid.Uuid

@KoinViewModel
class ActivityMemoryEditorViewModel(
    @InjectedParam private val params: Params,
    membersRepository: MembersRepository,
    memoriesRepository: MemoriesRepository,
    departmentsRepository: DepartmentsRepository,
    private val lendingsRemoteRepository: LendingsRemoteRepository,
    private val memoriesRemoteRepository: MemoriesRemoteRepository,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    data class Params(
        val lendingId: Uuid? = null,
        val memoryId: Uuid? = null,
    )

    val isForLending = params.lendingId != null

    val memory = (params.memoryId?.let { id -> memoriesRepository.getAsFlow(id) }
        ?: params.lendingId?.let { id -> memoriesRepository.getByLendingIdAsFlow(id) }
        ?: flowOf(null)
            ).stateInViewModel()

    /**
     * All active (non disabled) users.
     */
    val members = membersRepository.selectAllAsFlow()
        // We also check for null, because non-admins only get provided active users, and their status is not given (it is always active/null).
        .map { members -> members.filter { it.status == null || it.status == Member.Status.ACTIVE } }
        .stateInViewModel()

    val departments = departmentsRepository.selectAllAsFlow().stateInViewModel()

    val isSaving: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val saveProgress: StateFlow<Progress?>
        field = MutableStateFlow<Progress?>(null)

    val uploadSuccessful: StateFlow<Boolean>
        field = MutableStateFlow(false)

    fun save(
        from: ZonedDateTime?,
        to: ZonedDateTime?,
        place: String,
        members: List<Member>,
        externalUsers: String,
        sport: Sports?,
        department: Department?,
        text: RichTextState,
        files: List<PlatformFile>,
        /** Only applies when editing. Files removed that already exist on the memory. */
        removedFiles: List<Uuid>
    ) = launch {
        try {
            isSaving.value = true
            withContext(dispatcherProvider.io) {
                val markdownText = text.toMarkdown()
                if (isForLending) {
                    lendingsRemoteRepository.submitMemory(
                        params.lendingId!!,
                        place,
                        members,
                        externalUsers,
                        sport,
                        department,
                        markdownText,
                        files,
                        ProgressNotifier { saveProgress.value = it }
                    )
                } else {
                    require(from != null && to != null) { "From and To dates must be provided when creating a memory not for a lending" }

                    if (params.memoryId != null) {
                        memoriesRemoteRepository.patch(
                            params.memoryId,
                            place,
                            members,
                            externalUsers,
                            markdownText,
                            sport,
                            department,
                            files,
                            removedFiles,
                            from,
                            to,
                            ProgressNotifier { saveProgress.value = it },
                        )
                    } else {
                        memoriesRemoteRepository.create(
                            place,
                            members,
                            externalUsers,
                            markdownText,
                            sport,
                            department,
                            files,
                            from,
                            to,
                            ProgressNotifier { saveProgress.value = it },
                        )
                    }
                }
            }
            uploadSuccessful.value = true
        } finally {
            isSaving.value = false
        }
    }
}
