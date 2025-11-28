package org.centrexcursionistalcoi.app.ui.page.main.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.close
import coil3.compose.rememberAsyncImagePainter
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.m3.Markdown
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import org.centrexcursionistalcoi.app.data.rememberImageFiles
import org.centrexcursionistalcoi.app.ui.reusable.AsyncByteImage
import org.centrexcursionistalcoi.app.utils.isNullOrEmpty
import org.jetbrains.compose.resources.stringResource

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun FeedItem(
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
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp).padding(horizontal = 8.dp),
        )
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
