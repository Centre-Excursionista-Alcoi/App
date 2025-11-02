package org.centrexcursionistalcoi.app.ui.reusable

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.StateFlow
import org.centrexcursionistalcoi.app.process.Progress

/**
 * A linear loading indicator that shows a wavy animation when indeterminate,
 * and a progress bar when determinate.
 * @param progress The progress to display. If null, nothing is displayed.
 * If [Progress.Transfer] with null progress, an indeterminate indicator is shown.
 * If [Progress.Transfer] with non-null progress, a determinate indicator is shown.
 * @param modifier The modifier to be applied to the indicator.
 */
@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun LinearLoadingIndicator(progress: Progress?, modifier: Modifier = Modifier.fillMaxWidth()) {
    if (progress != null) {
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
}

@Composable
fun StateFlow<Progress?>.LinearLoadingIndicator(modifier: Modifier = Modifier.fillMaxWidth()) {
    val progress by collectAsState()
    LinearLoadingIndicator(progress, modifier)
}
