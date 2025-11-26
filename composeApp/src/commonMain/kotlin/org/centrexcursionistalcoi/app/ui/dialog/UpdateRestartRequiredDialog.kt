package org.centrexcursionistalcoi.app.ui.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import cea_app.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.platform.PlatformAppUpdates
import org.jetbrains.compose.resources.stringResource

@Composable
fun UpdateRestartRequiredDialog() {
    AlertDialog(
        onDismissRequest = { /*dialog cannot be dismissed*/ },
        title = { Text(stringResource(Res.string.update_restart_required_title)) },
        text = { Text(stringResource(Res.string.update_restart_required_message)) },
        confirmButton = {
            TextButton(
                onClick = { PlatformAppUpdates.onRestartRequested() }
            ) { Text(stringResource(Res.string.update_restart_required_action)) }
        },
    )
}
