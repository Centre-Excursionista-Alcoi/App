package org.centrexcursionistalcoi.app.ui.reusable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.centrexcursionistalcoi.app.process.Progress

@Composable
fun LoadingBox(paddingValues: PaddingValues = PaddingValues.Zero, progress: Progress? = null) {
    Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
        if (progress != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (progress is Progress.Transfer) {
                    val value = progress.progress
                    if (value == null) {
                        CircularProgressIndicator()
                    } else {
                        CircularProgressIndicator(progress = { value })
                    }
                } else {
                    CircularProgressIndicator()
                }

                progress.label()?.let { label ->
                    Text(
                        text = label,
                        modifier = Modifier.padding(top = 16.dp).widthIn(max = 300.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        } else {
            CircularProgressIndicator()
        }
    }
}
