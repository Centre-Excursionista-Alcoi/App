package org.centrexcursionistalcoi.app.ui.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cea_app.composeapp.generated.resources.*
import kotlinx.coroutines.Job
import org.jetbrains.compose.resources.stringResource

@Composable
fun <T> DeleteDialog(
    item: T,
    displayName: (T) -> String,
    onDelete: () -> Job,
    onDismissRequested: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismissRequested() },
        title = { Text(stringResource(Res.string.delete_dialog_title, displayName(item))) },
        text = { Text(stringResource(Res.string.delete_dialog_message, displayName(item))) },
        confirmButton = {
            TextButton(
                enabled = !isLoading,
                onClick = {
                    isLoading = true
                    onDelete().invokeOnCompletion { onDismissRequested() }
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) { Text(stringResource(Res.string.delete)) }
        },
        dismissButton = {
            TextButton(
                enabled = !isLoading,
                onClick = { onDismissRequested() }
            ) { Text(stringResource(Res.string.cancel)) }
        },
    )
}
