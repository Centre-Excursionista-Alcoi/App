package org.centrexcursionistalcoi.app.ui.page.main.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.m3.Markdown
import org.centrexcursionistalcoi.app.data.ReferencedPost
import org.centrexcursionistalcoi.app.data.localizedDate
import org.centrexcursionistalcoi.app.data.rememberImageFiles
import org.centrexcursionistalcoi.app.ui.reusable.AsyncByteImage
import org.jetbrains.compose.resources.stringResource

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PostItem(post: ReferencedPost) {
    var showingDialog by remember { mutableStateOf(false) }
    if (showingDialog) {
        ModalBottomSheet(
            onDismissRequest = { showingDialog = false },
        ) {
            val uriHandler = LocalUriHandler.current

            Text(
                text = post.title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 8.dp).padding(top = 8.dp),
            )
            Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                Text(
                    text = stringResource(Res.string.post_by, post.department?.displayName ?: stringResource(Res.string.post_department_generic)),
                )
                Text(" - ")
                Text(
                    text = post.localizedDate(),
                )
            }
            post.link?.let { link ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).clickable { uriHandler.openUri(link) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Link, null, tint = Color(0xff267ae8))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = link,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge.copy(color = Color(0xff267ae8), textDecoration = TextDecoration.Underline)
                    )
                }
            }

            Markdown(
                content = post.content,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                imageTransformer = Coil3ImageTransformerImpl,
            )

            if (post.files.isNotEmpty()) {
                val images = post.rememberImageFiles()
                LazyRow(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    item("start_spacer") { Spacer(Modifier.width(12.dp)) }
                    items(images.toList()) { (_, image) ->
                        AsyncByteImage(
                            bytes = image,
                            modifier = Modifier.fillMaxHeight().padding(horizontal = 8.dp),
                        )
                    }
                    item("end_spacer") { Spacer(Modifier.width(12.dp)) }
                }
            }
        }
    }

    OutlinedCard(
        modifier = Modifier.padding(8.dp),
        onClick = { showingDialog = true }
    ) {
        Text(
            text = post.title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp).padding(horizontal = 8.dp),
        )
        Text(
            text = post.localizedDate(),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 4.dp).padding(horizontal = 8.dp),
        )
        Text(
            text = post.content.take(128).let { content ->
                if (content.length >= 128) "$content..."
                else content
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp).padding(horizontal = 8.dp),
        )
    }
}
