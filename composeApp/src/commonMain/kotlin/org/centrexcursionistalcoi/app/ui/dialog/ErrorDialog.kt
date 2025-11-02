package org.centrexcursionistalcoi.app.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.GlobalAsyncErrorHandler
import org.centrexcursionistalcoi.app.platform.PlatformShareLogic
import org.centrexcursionistalcoi.app.ui.utils.orUnknown
import org.jetbrains.compose.resources.stringResource

@Composable
fun ErrorDialog(exception: Throwable? = null, message: String? = exception?.message, onDismissRequest: () -> Unit) {
    if (exception == null && message == null) {
        onDismissRequest()
    }

    AlertDialog(
        onDismissRequest = { GlobalAsyncErrorHandler.clearError() },
        title = { Text(stringResource(Res.string.error_dialog_title)) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                message?.let {
                    Text(stringResource(Res.string.error_dialog_message))
                    SelectionContainer {
                        Text(message)
                    }
                    Spacer(Modifier.height(8.dp))
                }

                exception?.let {
                    Text(stringResource(Res.string.error_dialog_type))
                    SelectionContainer {
                        Text(it::class.simpleName.orUnknown())
                    }
                    Spacer(Modifier.height(8.dp))

                    Text(stringResource(Res.string.error_dialog_stacktrace))
                    SelectionContainer {
                        Text(it.stackTraceToString())
                    }
                }
            }
        },
        confirmButton = {
            if (PlatformShareLogic.isSupported) {
                TextButton(
                    onClick = {
                        val msg = message ?: exception.toString()
                        PlatformShareLogic.share(msg)
                    }
                ) {
                    Text(stringResource(Res.string.share))
                }
            } else {
                TextButton(
                    onClick = onDismissRequest,
                ) {
                    Text(stringResource(Res.string.close))
                }
            }
        },
        dismissButton = {
            if (PlatformShareLogic.isSupported) {
                // Only show the dismiss button if sharing is available, because otherwise there would be two identical buttons
                TextButton(
                    onClick = onDismissRequest,
                ) {
                    Text(stringResource(Res.string.close))
                }
            }
        },
    )
}
