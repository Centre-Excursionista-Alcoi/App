package org.centrexcursionistalcoi.app.ui.page.main.activities

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.memories_empty
import cea_app.composeapp.generated.resources.memories_message
import cea_app.composeapp.generated.resources.memories_message_admin
import org.centrexcursionistalcoi.app.data.Memory
import org.jetbrains.compose.resources.stringResource

@Composable
fun MemoriesPage(isAdmin: Boolean, memories: List<Memory>) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            Text(
                text = if (isAdmin) stringResource(Res.string.memories_message_admin) else stringResource(Res.string.memories_message),
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
                    style = MaterialTheme.typography.labelLargeEmphasized
                )
            }
            items(memories, key = { it.id }, contentType = { "memory" }) { memory ->
                MemoryCard(memory)
            }
        }
    }
}

@Composable
fun MemoryCard(memory: Memory) {
    OutlinedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        val from = memory.from.toStringCompact()
        val to = memory.to.toStringCompact()
        Text("Dates: from $from until $to", modifier = Modifier.padding(8.dp))
        Text("Place: ${memory.place ?: "N/A"}", modifier = Modifier.padding(8.dp))
    }
}
