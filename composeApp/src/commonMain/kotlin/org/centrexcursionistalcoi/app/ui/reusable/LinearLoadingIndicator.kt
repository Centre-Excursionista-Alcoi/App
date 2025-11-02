package org.centrexcursionistalcoi.app.ui.reusable

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.StateFlow
import org.centrexcursionistalcoi.app.process.Progress

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun LinearLoadingIndicator(progress: Progress?, modifier: Modifier = Modifier) {
    if (progress is Progress.Transfer) {
        val value = progress.progress
        if (value == null) {
            LinearWavyProgressIndicator(modifier)
        } else {
            LinearWavyProgressIndicator(progress = { value }, modifier)
        }
    } else {
        LinearWavyProgressIndicator(modifier)
    }
}

@Composable
fun StateFlow<Progress?>.LinearLoadingIndicator(modifier: Modifier = Modifier) {
    val progress by collectAsState()
    LinearLoadingIndicator(progress, modifier)
}
