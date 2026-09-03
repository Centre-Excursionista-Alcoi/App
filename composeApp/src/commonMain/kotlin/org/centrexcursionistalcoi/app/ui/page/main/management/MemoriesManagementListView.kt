package org.centrexcursionistalcoi.app.ui.page.main.management

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.management_no_memories
import cea_app.composeapp.generated.resources.memory_create
import cea_app.composeapp.generated.resources.sort_by_date_asc
import cea_app.composeapp.generated.resources.sort_by_date_desc
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.Member
import org.centrexcursionistalcoi.app.data.ReferencedMemory
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.data.ZonedDateTime
import org.centrexcursionistalcoi.app.ui.dialog.MemoryDisplay
import org.centrexcursionistalcoi.app.ui.screen.MemoryEditor_Content
import org.centrexcursionistalcoi.app.viewmodel.management.MemoriesManagementViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MemoriesManagementListView(model: MemoriesManagementViewModel = koinViewModel()) {
    val departments by model.departments.collectAsState()
    val members by model.members.collectAsState()
    val memories by model.memories.collectAsState()

    MemoriesManagementListView(
        memories = memories,
        members = members,
        departments = departments,
        onDelete = model::delete
    )
}

@Composable
private fun MemoriesManagementListView(
    memories: List<ReferencedMemory>?,
    members: List<Member>?,
    departments: List<Department>?,
    onDelete: (ReferencedMemory) -> Job
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
            var from by remember { mutableStateOf<ZonedDateTime?>(null) }
            var to by remember { mutableStateOf<ZonedDateTime?>(null) }
            var place by remember { mutableStateOf("") }
            var memberUsers by remember { mutableStateOf<List<Member>>(emptyList()) }
            var externalUsers by remember { mutableStateOf("") }
            var sport by remember { mutableStateOf<Sports?>(null) }
            var department by remember { mutableStateOf<Department?>(null) }
            var files by remember { mutableStateOf<List<PlatformFile>>(emptyList()) }

            var isSaving by remember { mutableStateOf(false) }

            // TODO: Actually submit the edit

            MemoryEditor_Content(
                isForLending = false,
                isSaving = isSaving,
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
                files = files,
                onFromChange = { from = it },
                onToChange = { to = it },
                onPlaceChange = { place = it },
                onMemberUsersChange = { memberUsers = it },
                onExternalUsersChange = { externalUsers = it },
                onSportChange = { sport = it },
                onDepartmentChange = { department = it },
                onFilesChange = { files = it },
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            )
        },
    ) { memory ->
        MemoryDisplay(
            memory = memory
        )
    }
}
