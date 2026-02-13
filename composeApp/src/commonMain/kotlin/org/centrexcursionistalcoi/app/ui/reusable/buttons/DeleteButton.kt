package org.centrexcursionistalcoi.app.ui.reusable.buttons

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.delete
import cea_app.composeapp.generated.resources.delete_dialog_no_name_message
import cea_app.composeapp.generated.resources.delete_dialog_no_name_title
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.ui.dialog.DeleteDialog
import org.centrexcursionistalcoi.app.ui.dialog.DeleteDialogContext
import org.jetbrains.compose.resources.stringResource

@Composable
fun DeleteButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showReasonField: Boolean = false,
    requireConfirmation: Boolean = false,
    onClick: DeleteDialogContext?.() -> Unit
) {
    var showingDialog by remember { mutableStateOf(false) }
    if (showingDialog) {
        DeleteDialog(
            title = stringResource(Res.string.delete_dialog_no_name_title),
            message = stringResource(Res.string.delete_dialog_no_name_message),
            showReasonField = showReasonField,
            onDelete = {
                CoroutineScope(Dispatchers.IO).launch {
                    onClick()
                }
            },
            onDismissRequested = { showingDialog = false }
        )
    }

    TextButton(
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        onClick = {
            if (requireConfirmation) {
                showingDialog = true
            } else {
                onClick(null)
            }
        }
    ) {
        Text(stringResource(Res.string.delete))
    }
}

@Composable
fun <T> DeleteButton(
    item: T,
    modifier: Modifier = Modifier,
    displayName: (T) -> String? = { null },
    showReasonField: Boolean = false,
    enabled: Boolean = true,
    onClick: DeleteDialogContext.() -> Job
) {
    var showingDialog by remember { mutableStateOf(false) }
    if (showingDialog) {
        DeleteDialog(
            item = item,
            displayName = displayName,
            showReasonField = showReasonField,
            onDelete = onClick,
            onDismissRequested = { showingDialog = false }
        )
    }

    TextButton(
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        onClick = { showingDialog = true }
    ) {
        Text(stringResource(Res.string.delete))
    }
}
