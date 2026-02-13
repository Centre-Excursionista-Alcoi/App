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
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.close
import cea_app.composeapp.generated.resources.delete
import cea_app.composeapp.generated.resources.delete_dialog_message
import cea_app.composeapp.generated.resources.delete_dialog_no_name_message
import cea_app.composeapp.generated.resources.delete_dialog_no_name_title
import cea_app.composeapp.generated.resources.delete_dialog_title
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.ui.data.IDialogContext
import org.jetbrains.compose.resources.stringResource

@Composable
fun <T> DeleteDialog(
    item: T,
    displayName: (T) -> String?,
    onDelete: () -> Job,
    onDismissRequested: () -> Unit
) {
    val name = displayName(item)

    DeleteDialog(
        title = if (name != null) {
            stringResource(Res.string.delete_dialog_title, name)
        } else {
            stringResource(Res.string.delete_dialog_no_name_title)
        },
        message = if (name != null) {
            stringResource(Res.string.delete_dialog_message, name)
        } else {
            stringResource(Res.string.delete_dialog_no_name_message)
        },
        onDelete = { onDelete().also { it.invokeOnCompletion { dismiss() } } },
        onDismissRequested = onDismissRequested
    )
}

@Composable
fun DeleteDialog(
    title: String,
    message: String,
    buttonText: String = stringResource(Res.string.delete),
    onDelete: IDialogContext.() -> Job,
    onDismissRequested: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismissRequested() },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                enabled = !isLoading,
                onClick = {
                    isLoading = true
                    onDelete(
                        object : IDialogContext {
                            override fun dismiss() {
                                onDismissRequested()
                            }
                        }
                    ).invokeOnCompletion { isLoading = false }
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) { Text(buttonText) }
        },
        dismissButton = {
            TextButton(
                enabled = !isLoading,
                onClick = { onDismissRequested() }
            ) { Text(stringResource(Res.string.close)) }
        },
    )
}
