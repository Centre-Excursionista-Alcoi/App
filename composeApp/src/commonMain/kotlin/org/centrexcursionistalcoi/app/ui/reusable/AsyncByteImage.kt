package org.centrexcursionistalcoi.app.ui.reusable

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Close
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Warning
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
                    Icon(MaterialSymbols.Warning, null, modifier = Modifier.align(Alignment.Center))
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
                                        imageVector = MaterialSymbols.Close,
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
