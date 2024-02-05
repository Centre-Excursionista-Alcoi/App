package ui.dialog

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.Job
import ui.reusable.form.FormField

@Composable
fun TextInputDialog(
    title: String,
    label: String,
    initialValue: String = "",
    onValueChange: ((String) -> Job)?,
    onDismissRequest: () -> Unit
) {
    var text by remember { mutableStateOf(TextFieldValue(initialValue)) }

    CoroutineDialog(
        title = title,
        onDismissRequest = onDismissRequest,
        onSubmit = onValueChange?.let { { it(text.text) } }
    ) { isLoading ->
        FormField(
            value = text,
            onValueChange = { text = it },
            label = label,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            onSubmit = ::submit
        )
    }
}
