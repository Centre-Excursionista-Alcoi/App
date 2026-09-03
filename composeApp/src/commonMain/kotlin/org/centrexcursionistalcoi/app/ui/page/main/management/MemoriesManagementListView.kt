package org.centrexcursionistalcoi.app.ui.page.main.management

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.management_no_memories
import cea_app.composeapp.generated.resources.memory_create
import cea_app.composeapp.generated.resources.sort_by_date_asc
import cea_app.composeapp.generated.resources.sort_by_date_desc
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.data.ReferencedMemory
import org.centrexcursionistalcoi.app.ui.dialog.MemoryDisplay
import org.centrexcursionistalcoi.app.viewmodel.management.MemoriesManagementViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MemoriesManagementListView(model: MemoriesManagementViewModel = koinViewModel()) {
    val memories by model.memories.collectAsState()

    MemoriesManagementListView(
        memories = memories,
        onDelete = model::delete
    )
}

@Composable
private fun MemoriesManagementListView(
    memories: List<ReferencedMemory>?,
    onDelete: (ReferencedMemory) -> Job
) {
    ListView(
        items = memories,
        itemIdProvider = { it.id },
        // TODO: Improve display name
        itemDisplayName = { it.from.toStringCompact() + " -> " + it.to.toStringCompact() },
        itemSupportingContent = { memory ->
            Text(memory.submittedBy.fullName)
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
            // TODO: Show proper edit content
            Text("Work in progress")
        },
    ) { memory ->
        MemoryDisplay(
            memory = memory
        )
    }
}
