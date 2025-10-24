package org.centrexcursionistalcoi.app.ui.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import cea_app.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun LogoutConfirmationDialog(
    onLogoutRequested: () -> Unit,
    onDismissRequested: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismissRequested() },
        title = { Text(stringResource(Res.string.logout_confirmation_title)) },
        text = { Text(stringResource(Res.string.logout_confirmation_message)) },
        confirmButton = {
            TextButton(
                onClick = onLogoutRequested
            ) { Text(stringResource(Res.string.logout)) }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequested
            ) { Text(stringResource(Res.string.cancel)) }
        },
    )
}
