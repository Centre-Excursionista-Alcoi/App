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
import cea_app.composeapp.generated.resources.memories_empty
import cea_app.composeapp.generated.resources.memories_message
import cea_app.composeapp.generated.resources.memory_from
import cea_app.composeapp.generated.resources.memory_place
import cea_app.composeapp.generated.resources.memory_to
import org.centrexcursionistalcoi.app.data.ReferencedMemory
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.ui.dialog.MemoryDialog
import org.centrexcursionistalcoi.app.ui.icons.material.CalendarEndOutline
import org.centrexcursionistalcoi.app.ui.icons.material.CalendarStartOutline
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Location
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.viewmodel.MemoriesViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Whether [profile] is allowed to modify this memory: only its submitter, or an admin. Tagged members can only view it. */
private fun ReferencedMemory.canBeEditedBy(profile: ProfileResponse): Boolean =
    submittedBy == profile || profile.isAdmin

@Composable
fun MemoriesPage(
    model: MemoriesViewModel = koinViewModel(),
    onEditRequest: (ReferencedMemory) -> Unit
) {
    val memories by model.memories.collectAsState()
    val profile by model.profile.collectAsState()

    memories?.let { MemoriesPage(memories = it, profile = profile, onEditRequest) } ?: LoadingBox()
}

@Composable
private fun MemoriesPage(memories: List<ReferencedMemory>, profile: ProfileResponse?, onEditRequest: (ReferencedMemory) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                text = stringResource(Res.string.memories_message),
                modifier = Modifier.padding(8.dp)
            )
        }

        if (memories.isEmpty()) item {
            Text(stringResource(Res.string.memories_empty), modifier = Modifier.padding(8.dp))
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
                val canEdit = profile != null && memory.canBeEditedBy(profile)
                MemoryCard(memory, onEditRequest = { onEditRequest(memory) }.takeIf { canEdit })
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
