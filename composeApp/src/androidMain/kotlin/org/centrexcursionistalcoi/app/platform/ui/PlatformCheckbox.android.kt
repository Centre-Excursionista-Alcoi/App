package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun PlatformCheckbox(
    checked: Boolean,
    onCheckedChanged: (Boolean) -> Unit,
    label: String?,
    modifier: Modifier,
    enabled: Boolean
) {
    Row(
        modifier = modifier
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChanged,
            enabled = enabled
        )
        label?.let { Text(text = it, style = MaterialTheme.typography.labelMedium) }
    }
}
