package org.centrexcursionistalcoi.app.ui.page.main.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.close
import cea_app.composeapp.generated.resources.post_by
import cea_app.composeapp.generated.resources.post_department_generic
import coil3.compose.rememberAsyncImagePainter
import com.mikepenz.markdown.m3.Markdown
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import org.centrexcursionistalcoi.app.data.ReferencedPost
import org.centrexcursionistalcoi.app.data.localizedDate
import org.centrexcursionistalcoi.app.data.rememberImageFiles
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Close
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Link
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Newsmode
import org.centrexcursionistalcoi.app.ui.reusable.AsyncByteImage
import org.jetbrains.compose.resources.stringResource

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PostItem(post: ReferencedPost) {
    val uriHandler = LocalUriHandler.current

    FeedItem(
        icon = MaterialSymbols.Newsmode,
        title = post.title,
        dateString = post.localizedDate(),
        content = post.content,
        dialogContent = {
            val publisherText = stringResource(Res.string.post_by, post.department?.displayName ?: stringResource(Res.string.post_department_generic))

            Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                Text(
                    text = publisherText,
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
                    Icon(MaterialSymbols.Link, null, tint = Color(0xff267ae8))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = link,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge.copy(color = Color(0xff267ae8), textDecoration = TextDecoration.Underline)
                    )
                }
            }

            Markdown(post.content, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))

            if (post.files.isNotEmpty()) {
                val images = post.rememberImageFiles()
                LazyRow(
                    modifier = Modifier.fillMaxWidth().height(300.dp).padding(top = 12.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    item("start_spacer") { Spacer(Modifier.width(12.dp)) }
                    items(images.toList()) { (_, image) ->
                        var showingDialog by remember { mutableStateOf(false) }
                        if (showingDialog) {
                            Dialog(
                                onDismissRequest = { showingDialog = false },
                                properties = DialogProperties(
                                    usePlatformDefaultWidth = false
                                )
                            ) {
                                val painter = rememberAsyncImagePainter(image)
                                val zoomState = rememberZoomState(contentSize = painter.intrinsicSize)
                                Box(modifier = Modifier.fillMaxSize()) {
                                    IconButton(
                                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp).zIndex(1f),
                                        onClick = { showingDialog = false },
                                    ) {
                                        Icon(
                                            imageVector = MaterialSymbols.Close,
                                            contentDescription = stringResource(Res.string.close),
                                            tint = Color.White
                                        )
                                    }

                                    Image(
                                        painter = painter,
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize().zoomable(zoomState)
                                    )
                                }
                            }
                        }

                        AsyncByteImage(
                            bytes = image,
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 8.dp),
                            canBeMaximized = true,
                        )
                    }
                    item("end_spacer") { Spacer(Modifier.width(12.dp)) }
                }
            }
        }
    )
}
