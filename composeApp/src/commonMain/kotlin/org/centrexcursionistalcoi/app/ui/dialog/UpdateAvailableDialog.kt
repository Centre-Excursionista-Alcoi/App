package org.centrexcursionistalcoi.app.ui.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import cea_app.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.platform.PlatformAppUpdates
import org.jetbrains.compose.resources.stringResource

@Composable
fun UpdateAvailableDialog() {
    AlertDialog(
        onDismissRequest = { PlatformAppUpdates.dismissUpdateAvailable() },
        title = { Text(stringResource(Res.string.update_available_title)) },
        text = { Text(stringResource(Res.string.update_available_message)) },
        confirmButton = {
            TextButton(
                onClick = { PlatformAppUpdates.startUpdate() }
            ) { Text(stringResource(Res.string.update_available_action)) }
        },
    )
}
