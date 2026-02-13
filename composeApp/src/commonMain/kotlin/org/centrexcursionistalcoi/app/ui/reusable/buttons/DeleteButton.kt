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
import org.jetbrains.compose.resources.stringResource

@Composable
fun DeleteButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    requireConfirmation: Boolean = false,
    onClick: () -> Unit
) {
    var showingDialog by remember { mutableStateOf(false) }
    if (showingDialog) {
        DeleteDialog(
            title = stringResource(Res.string.delete_dialog_no_name_title),
            message = stringResource(Res.string.delete_dialog_no_name_message),
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
                onClick()
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
    enabled: Boolean = true,
    onClick: () -> Job
) {
    var showingDialog by remember { mutableStateOf(false) }
    if (showingDialog) {
        DeleteDialog(
            item = item,
            displayName = displayName,
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
