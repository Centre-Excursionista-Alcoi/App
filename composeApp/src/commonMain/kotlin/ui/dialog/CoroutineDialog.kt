package ui.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.Job
import resources.MR

interface CoroutineDialogContext {
    fun dismiss()

    fun submit()
}

@Composable
fun CoroutineDialog(
    title: String,
    onDismissRequest: () -> Unit,
    onSubmit: (() -> Job)?,
    content: @Composable CoroutineDialogContext.(isLoading: Boolean) -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }

    val context = object : CoroutineDialogContext {
        override fun dismiss() { onDismissRequest() }
        override fun submit() {
            isLoading = true
            onSubmit?.invoke()?.invokeOnCompletion {
                isLoading = false
                onDismissRequest()
            } ?: run {
                isLoading = false
                onDismissRequest()
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (isLoading) onDismissRequest() },
        title = { Text(title) },
        text = { content(context, isLoading) },
        confirmButton = {
            TextButton(
                enabled = !isLoading,
                onClick = context::submit
            ) { Text(stringResource(MR.strings.ok)) }
        },
        dismissButton = {
            TextButton(
                enabled = !isLoading,
                onClick = onDismissRequest
            ) { Text(stringResource(MR.strings.cancel)) }
        }
    )
}
