package org.centrexcursionistalcoi.app.viewmodel.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohamedrejeb.richeditor.model.RichTextState
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.Member
import org.centrexcursionistalcoi.app.data.ReferencedMemory
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.data.ZonedDateTime
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.MembersRepository
import org.centrexcursionistalcoi.app.database.MemoriesRepository
import org.centrexcursionistalcoi.app.di.DispatcherProvider
import org.centrexcursionistalcoi.app.network.MemoriesRemoteRepository
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.viewmodel.launch
import org.centrexcursionistalcoi.app.viewmodel.stateInViewModel
import org.koin.core.annotation.KoinViewModel
import kotlin.uuid.Uuid

@KoinViewModel
class MemoriesManagementViewModel(
    private val dispatcherProvider: DispatcherProvider,
    departmentsRepository: DepartmentsRepository,
    memoriesRepository: MemoriesRepository,
    membersRepository: MembersRepository,
    private val memoriesRemoteRepository: MemoriesRemoteRepository,
) : ViewModel() {
    val departments = departmentsRepository.selectAllAsFlow().stateInViewModel()
    val members = membersRepository.selectAllAsFlow().stateInViewModel()

    val memories = memoriesRepository.selectAllAsFlow()
        .map { list ->
            // sort by date, showing newest first
            list.sortedByDescending { it.from }
        }
        .stateInViewModel()

    val saveProgress: StateFlow<Progress?>
        field = MutableStateFlow(null)

    fun delete(memory: ReferencedMemory) = viewModelScope.launch(dispatcherProvider.io) {
        memoriesRemoteRepository.delete(memory.id)
    }

    fun save(
        memory: ReferencedMemory?,
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
        removedFiles: List<Uuid>,
        onSuccess: () -> Unit,
    ) = launch {
        try {
            saveProgress.value = Progress.Default
            withContext(dispatcherProvider.io) {
                require(from != null && to != null) { "From and To dates must be provided when creating a memory not for a lending" }

                val markdownText = text.toMarkdown()

                if (memory != null) {
                    memoriesRemoteRepository.patch(
                        memory.id,
                        place.takeIf { memory.place != it },
                        members.takeIf { memory.members != it },
                        externalUsers.takeIf { memory.externalUsers != it },
                        markdownText.takeIf { memory.text != it },
                        sport.takeIf { memory.sport != it },
                        department.takeIf { memory.department != it },
                        files.takeIf { memory.files != it },
                        removedFiles,
                        from.takeIf { memory.from != it },
                        to.takeIf { memory.to != it },
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

                onSuccess()
            }
        } finally {
            saveProgress.value = null
        }
    }
}
