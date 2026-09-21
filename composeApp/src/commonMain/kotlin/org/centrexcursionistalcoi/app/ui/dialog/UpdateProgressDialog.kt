package org.centrexcursionistalcoi.app.ui.dialog

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.update_available_action
import cea_app.composeapp.generated.resources.update_download_progress_title
import org.centrexcursionistalcoi.app.platform.PlatformAppUpdates
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun UpdateProgressDialog() {
    val progress by PlatformAppUpdates.updateProgress.collectAsState(null)

    AlertDialog(
        onDismissRequest = { /* dialog cannot be dismissed */ },
        title = { Text(stringResource(Res.string.update_download_progress_title)) },
        text = {
            Box(contentAlignment = Alignment.Center) {
                progress?.takeIf { it > 0 && it < 1 }?.let {
                    CircularWavyProgressIndicator(
                        progress = { it },
                    )
                } ?: CircularWavyProgressIndicator()

                progress?.let {
                    Text("${(it*100).roundToInt()} %")
                }
            }
        },
        confirmButton = {
            if (progress == null) {
                TextButton(
                    onClick = { PlatformAppUpdates.startUpdate() }
                ) { Text(stringResource(Res.string.update_available_action)) }
            }
        },
    )
}
