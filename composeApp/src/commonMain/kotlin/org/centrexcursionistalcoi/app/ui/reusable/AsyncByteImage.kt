package org.centrexcursionistalcoi.app.ui.reusable

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import io.github.aakira.napier.Napier

@Composable
fun AsyncByteImage(bytes: ByteArray?, contentDescription: String? = null, modifier: Modifier = Modifier) {
    Box(modifier) {
        bytes?.let {
            if (bytes.isEmpty()) {
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
                        Napier.e((state as AsyncImagePainter.State.Error).result.throwable) {
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
                        Image(
                            painter = painter,
                            contentDescription = contentDescription,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        } ?: CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }
}
