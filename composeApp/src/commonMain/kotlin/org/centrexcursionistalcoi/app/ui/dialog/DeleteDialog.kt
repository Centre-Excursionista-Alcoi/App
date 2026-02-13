package org.centrexcursionistalcoi.app.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.close
import cea_app.composeapp.generated.resources.delete
import cea_app.composeapp.generated.resources.delete_dialog_message
import cea_app.composeapp.generated.resources.delete_dialog_no_name_message
import cea_app.composeapp.generated.resources.delete_dialog_no_name_title
import cea_app.composeapp.generated.resources.delete_dialog_reason
import cea_app.composeapp.generated.resources.delete_dialog_title
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.ui.data.IDialogContext
import org.centrexcursionistalcoi.app.ui.utils.optional
import org.jetbrains.compose.resources.stringResource

interface DeleteDialogContext : IDialogContext {
    val reason: String?
}

@Composable
fun <T> DeleteDialog(
    item: T,
    displayName: (T) -> String?,
    showReasonField: Boolean = false,
    onDelete: DeleteDialogContext.() -> Job,
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
        showReasonField = showReasonField,
        onDelete = { onDelete().also { it.invokeOnCompletion { dismiss() } } },
        onDismissRequested = onDismissRequested
    )
}

@Composable
fun DeleteDialog(
    title: String,
    message: String,
    buttonText: String = stringResource(Res.string.delete),
    showReasonField: Boolean = false,
    onDelete: DeleteDialogContext.() -> Job,
    onDismissRequested: () -> Unit
) {
    var reason by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismissRequested() },
        title = { Text(title) },
        text = {
            Column {
                Text(message)

                if (showReasonField) {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text(stringResource(Res.string.delete_dialog_reason).optional()) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isLoading,
                onClick = {
                    isLoading = true
                    onDelete(
                        object : DeleteDialogContext {
                            override val reason: String? = reason.takeIf { showReasonField }

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
