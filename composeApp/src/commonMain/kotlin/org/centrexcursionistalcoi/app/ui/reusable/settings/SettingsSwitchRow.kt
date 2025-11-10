package org.centrexcursionistalcoi.app.ui.reusable.settings

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    summary: String? = null,
    icon: ImageVector? = null,
    contentDescription: String? = title,
) {
    SettingsRow(
        title = title,
        summary = summary,
        icon = icon,
        contentDescription = contentDescription,
        trailingContent = {
            Switch(checked, onCheckedChange)
        },
        onClick = { onCheckedChange(!checked) }
    )
}
