package org.centrexcursionistalcoi.app.ui.dialog

import androidx.compose.material3.*
import androidx.compose.runtime.*
import cea_app.composeapp.generated.resources.*
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Warning
import org.jetbrains.compose.resources.stringResource

@Composable
fun RemoveAccountDialog(
    onConfirm: () -> Job,
    onDismissRequest: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.errorContainer,
        titleContentColor = MaterialTheme.colorScheme.onErrorContainer,
        iconContentColor = MaterialTheme.colorScheme.onErrorContainer,
        textContentColor = MaterialTheme.colorScheme.onErrorContainer,
        onDismissRequest = { if (!isLoading) onDismissRequest() },
        title = { Text(stringResource(Res.string.settings_remove_account_title)) },
        icon = { Icon(MaterialSymbols.Warning, stringResource(Res.string.settings_remove_account_title)) },
        text = { Text(stringResource(Res.string.settings_remove_account_message)) },
        confirmButton = {
            TextButton(
                enabled = !isLoading,
                onClick = {
                    isLoading = true
                    onConfirm()
                    isLoading = false
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) { Text(stringResource(Res.string.confirm)) }
        },
        dismissButton = {
            TextButton(
                enabled = !isLoading,
                onClick = { onDismissRequest() }
            ) { Text(stringResource(Res.string.close)) }
        },
    )
}
