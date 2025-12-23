package org.centrexcursionistalcoi.app.ui.page.main.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun FeedItem(
    icon: ImageVector,
    title: String,
    dateString: String,
    content: String?,
    dialogContent: @Composable ColumnScope.() -> Unit,
) {
    var showingDialog by remember { mutableStateOf(false) }
    if (showingDialog) {
        ModalBottomSheet(
            onDismissRequest = { showingDialog = false },
        ) {
            Column(Modifier.padding(8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                dialogContent()
            }
        }
    }

    OutlinedCard(
        modifier = Modifier.padding(8.dp),
        onClick = { showingDialog = true }
    ) {
        Row(
            modifier = Modifier.padding(top = 8.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null)
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
        }
        Text(
            text = dateString,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 8.dp).padding(horizontal = 8.dp),
        )
        if (content != null) {
            Text(
                text = content.take(128).let { content ->
                    if (content.length >= 128) "$content..."
                    else content
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp).padding(horizontal = 8.dp),
            )
        }
    }
}
