package org.centrexcursionistalcoi.app.ui.dialog

import androidx.compose.material3.*
import androidx.compose.runtime.*
import cea_app.composeapp.generated.resources.*
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.ui.data.IDialogContext
import org.jetbrains.compose.resources.stringResource

@Composable
fun <T> DeleteDialog(
    item: T,
    displayName: (T) -> String,
    onDelete: () -> Job,
    onDismissRequested: () -> Unit
) {
    DeleteDialog(
        title = stringResource(Res.string.delete_dialog_title, displayName(item)),
        message = stringResource(Res.string.delete_dialog_message, displayName(item)),
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
