package org.centrexcursionistalcoi.app.ui.page.main.management

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.management_no_memories
import cea_app.composeapp.generated.resources.memory_create
import cea_app.composeapp.generated.resources.save
import cea_app.composeapp.generated.resources.sort_by_date_asc
import cea_app.composeapp.generated.resources.sort_by_date_desc
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.Member
import org.centrexcursionistalcoi.app.data.ReferencedMemory
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.data.ZonedDateTime
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.ui.dialog.MemoryDisplay
import org.centrexcursionistalcoi.app.ui.screen.MemoryEditor_Content
import org.centrexcursionistalcoi.app.viewmodel.management.MemoriesManagementViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.uuid.Uuid

@Composable
fun MemoriesManagementListView(
    snackbarHostState: SnackbarHostState? = null,
    model: MemoriesManagementViewModel = koinViewModel()
) {
    val progress by model.saveProgress.collectAsState()

    val departments by model.departments.collectAsState()
    val members by model.members.collectAsState()
    val memories by model.memories.collectAsState()

    MemoriesManagementListView(
        memories = memories,
        members = members,
        departments = departments,
        onDelete = model::delete,
        snackbarHostState = snackbarHostState,
        progress = progress,
        onSave = model::save
    )
}

@Composable
private fun MemoriesManagementListView(
    memories: List<ReferencedMemory>?,
    members: List<Member>?,
    departments: List<Department>?,
    onDelete: (ReferencedMemory) -> Job,
    snackbarHostState: SnackbarHostState?,
    progress: Progress?,
    onSave: (memory: ReferencedMemory?, from: ZonedDateTime?, to: ZonedDateTime?, place: String, memberUsers: List<Member>, externalUsers: String, sport: Sports?, department: Department?, description: RichTextState, files: List<PlatformFile>, removedFiles: List<Uuid>, onSuccess: () -> Unit) -> Unit,
) {
    ListView(
        items = memories,
        itemIdProvider = { it.id },
        itemDisplayName = { memory ->
            val text = StringBuilder(memory.submittedBy.fullName)
            if (memory.place != null) {
                text.append(" - ")
                text.append(memory.place)
            }
            text.toString()
        },
        itemSupportingContent = {
            Text(it.from.toStringCompact() + " -> " + it.to.toStringCompact())
        },
        sortByOptions = listOf(
            SortBy.desc({ stringResource(Res.string.sort_by_date_desc) }) { it.from },
            SortBy.asc({ stringResource(Res.string.sort_by_date_asc) }) { it.from }
        ),
        emptyItemsText = stringResource(Res.string.management_no_memories),
        isCreatingSupported = false,
        createTitle = stringResource(Res.string.memory_create),
        onDeleteRequest = onDelete,
        editItemContent = { memory ->
            val state = rememberRichTextState()
            var from by remember(memory) { mutableStateOf(memory?.from) }
            var to by remember(memory) { mutableStateOf(memory?.to) }
            var place by remember(memory) { mutableStateOf(memory?.place.orEmpty()) }
            var memberUsers by remember(memory) { mutableStateOf(memory?.members.orEmpty()) }
            var externalUsers by remember(memory) { mutableStateOf(memory?.externalUsers.orEmpty()) }
            var sport by remember(memory) { mutableStateOf(memory?.sport) }
            var department by remember(memory) { mutableStateOf(memory?.department) }

            var images by remember(memory) { mutableStateOf<List<PlatformFile>>(emptyList()) }
            var removedImages by remember { mutableStateOf<List<Uuid>>(emptyList()) }

            LaunchedEffect(memory) {
                memory?.let {
                    state.setMarkdown(it.text)
                }
            }

            MemoryEditor_Content(
                isForLending = false,
                isSaving = progress != null,
                members = members,
                departments = departments,
                state = state,
                from = from,
                to = to,
                place = place,
                memberUsers = memberUsers,
                externalUsers = externalUsers,
                sport = sport,
                department = department,
                previousImagesContainer = memory,
                removedPreviousImages = removedImages,
                images = images,
                onFromChange = { from = it },
                onToChange = { to = it },
                onPlaceChange = { place = it },
                onMemberUsersChange = { memberUsers = it },
                onExternalUsersChange = { externalUsers = it },
                onSportChange = { sport = it },
                onDepartmentChange = { department = it },
                onImagesChange = { images = it },
                onModifyRemovedPreviousImages = { removedImages = it },
            )

            OutlinedButton(
                enabled = progress == null,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                onClick = {
                    onSave(
                        memory,
                        from,
                        to,
                        place,
                        memberUsers,
                        externalUsers,
                        sport,
                        department,
                        state,
                        images,
                        removedImages
                    ) {
                        finishEdit()
                    }
                }
            ) { Text(text = stringResource(Res.string.save)) }
        },
    ) { memory ->
        MemoryDisplay(
            memory = memory,
            snackbarHostState = snackbarHostState,
        )
    }
}
