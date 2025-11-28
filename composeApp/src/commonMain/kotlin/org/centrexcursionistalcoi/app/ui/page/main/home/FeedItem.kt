package org.centrexcursionistalcoi.app.ui.page.main.home

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.m3.Markdown

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun FeedItem(
    icon: ImageVector,
    title: String,
    dateString: String,
    content: String?,
    publisherText: String,
    dialogBelowTitle: @Composable ColumnScope.() -> Unit = {},
    dialogBottom: @Composable ColumnScope.() -> Unit = {},
) {
    var showingDialog by remember { mutableStateOf(false) }
    if (showingDialog) {
        ModalBottomSheet(
            onDismissRequest = { showingDialog = false },
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 8.dp).padding(top = 8.dp),
            )
            Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                Text(
                    text = publisherText,
                )
                Text(" - ")
                Text(
                    text = dateString,
                )
            }
            dialogBelowTitle()

            if (content != null) {
                Markdown(
                    content = content,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    imageTransformer = Coil3ImageTransformerImpl,
                )
            }

            dialogBottom()
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
