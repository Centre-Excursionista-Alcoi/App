package org.centrexcursionistalcoi.app.ui.reusable.form

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ReadOnlyFormField(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        label = { Text(label) },
        singleLine = true,
        readOnly = true,
        modifier = modifier,
        isError = error != null,
        supportingText = error?.let {
            { Text(it) }
        },
    )
}
