package org.centrexcursionistalcoi.app.ui.page.main.activities

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.activities_empty
import cea_app.composeapp.generated.resources.activities_message
import cea_app.composeapp.generated.resources.memories_empty
import cea_app.composeapp.generated.resources.memories_message
import cea_app.composeapp.generated.resources.memory_from
import cea_app.composeapp.generated.resources.memory_place
import cea_app.composeapp.generated.resources.memory_to
import org.centrexcursionistalcoi.app.data.ReferencedMemory
import org.centrexcursionistalcoi.app.ui.dialog.MemoryDialog
import org.centrexcursionistalcoi.app.ui.icons.material.CalendarEndOutline
import org.centrexcursionistalcoi.app.ui.icons.material.CalendarStartOutline
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Location
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.viewmodel.MemoriesViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** The memories submitted by the current user. Always editable by them. */
@Composable
fun MemoriesPage(
    model: MemoriesViewModel = koinViewModel(),
    onEditRequest: (ReferencedMemory) -> Unit
) {
    val memories by model.memories.collectAsState()

    memories?.let {
        MemoryListContent(
            memories = it,
            message = stringResource(Res.string.memories_message),
            emptyMessage = stringResource(Res.string.memories_empty),
            onEditRequest = onEditRequest,
        )
    } ?: LoadingBox()
}

/**
 * The memories the current user is tagged as a participant on, but didn't submit themselves -- read-only, since
 * only the submitter (or an admin) can modify a memory. This is what distinguishes this "Activities" tab from the
 * "Memories" one: memories here can be read but never edited.
 */
@Composable
fun TaggedMemoriesPage(model: MemoriesViewModel = koinViewModel()) {
    val taggedMemories by model.taggedMemories.collectAsState()

    taggedMemories?.let {
        MemoryListContent(
            memories = it,
            message = stringResource(Res.string.activities_message),
            emptyMessage = stringResource(Res.string.activities_empty),
            onEditRequest = null,
        )
    } ?: LoadingBox()
}

@Composable
private fun MemoryListContent(
    memories: List<ReferencedMemory>,
    message: String,
    emptyMessage: String,
    onEditRequest: ((ReferencedMemory) -> Unit)?,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                text = message,
                modifier = Modifier.padding(8.dp)
            )
        }

        if (memories.isEmpty()) item {
            Text(emptyMessage, modifier = Modifier.padding(8.dp))
        }

        val groupedMemories = memories.groupBy { it.from.date.year }
        for ((year, memories) in groupedMemories) {
            stickyHeader(key = "header_$year", contentType = "header") {
                Text(
                    text = year.toString(),
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    fontWeight = FontWeight.Bold,
                )
            }
            items(memories, key = { it.id }, contentType = { "memory" }) { memory ->
                MemoryCard(memory, onEditRequest = onEditRequest?.let { callback -> { callback(memory) } })
            }
        }
    }
}

@Composable
fun MemoryCard(memory: ReferencedMemory, onEditRequest: (() -> Unit)? = null) {
    var showingDialog by remember { mutableStateOf(false) }
    if (showingDialog) {
        MemoryDialog(
            memory = memory,
            onEditRequest = onEditRequest,
            onDismissRequest = { showingDialog = false }
        )
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        onClick = { showingDialog = true }
    ) {
        val from = memory.from.toStringCompact()
        val to = memory.to.toStringCompact()
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).padding(top = 4.dp)) {
            Icon(
                imageVector = MaterialSymbols.CalendarStartOutline,
                contentDescription = stringResource(Res.string.memory_from),
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = from,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = MaterialSymbols.CalendarEndOutline,
                contentDescription = stringResource(Res.string.memory_to),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Text(
                text = to,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
        }
        memory.place?.let { place ->
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                Icon(
                    imageVector = MaterialSymbols.Location,
                    contentDescription = stringResource(Res.string.memory_place),
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = place,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        memory.attachments.takeIf { it.isNotEmpty() }?.let { attachments ->
            Badge(modifier = Modifier.padding(8.dp)) { Text("${attachments.size} files") }
        }
    }
}
