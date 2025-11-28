package org.centrexcursionistalcoi.app.ui.reusable

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.diamondedge.logging.logging
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import org.centrexcursionistalcoi.app.data.rememberImageFile
import org.jetbrains.compose.resources.stringResource

private val log = logging()

@Composable
fun AsyncByteImage(
    bytes: ByteArray?,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    canBeMaximized: Boolean = false,
) {
    Box(modifier) {
        if (bytes == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (bytes.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Warning, null, modifier = Modifier.align(Alignment.Center))
                }
            }
        } else {
            val painter = rememberAsyncImagePainter(bytes)
            val state by painter.state.collectAsState()
            when (state) {
                is AsyncImagePainter.State.Error -> {
                    log.e((state as AsyncImagePainter.State.Error).result.throwable) {
                        "Could not load image."
                    }
                    Text(
                        "Could not load image.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is AsyncImagePainter.State.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                AsyncImagePainter.State.Empty -> {
                    Text(
                        "Image is empty",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    // Success
                    var showingDialog by remember { mutableStateOf(false) }
                    if (showingDialog) {
                        Dialog(
                            onDismissRequest = { showingDialog = false },
                            properties = DialogProperties(
                                usePlatformDefaultWidth = false
                            )
                        ) {
                            val zoomState = rememberZoomState(contentSize = painter.intrinsicSize)
                            Box(modifier = Modifier.fillMaxSize()) {
                                IconButton(
                                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp).zIndex(1f),
                                    onClick = { showingDialog = false },
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(Res.string.close),
                                        tint = Color.White
                                    )
                                }

                                Image(
                                    painter = painter,
                                    contentDescription = contentDescription,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize().zoomable(zoomState)
                                )
                            }
                        }
                    }

                    Image(
                        painter = painter,
                        contentDescription = contentDescription,
                        modifier = Modifier.fillMaxSize().then(
                            if (canBeMaximized)
                                Modifier.clickable { showingDialog = true }
                            else
                                Modifier
                        ),
                        contentScale = contentScale,
                    )
                }
            }
        }
    }
}
