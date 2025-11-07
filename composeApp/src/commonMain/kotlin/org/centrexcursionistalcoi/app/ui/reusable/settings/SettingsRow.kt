package org.centrexcursionistalcoi.app.ui.reusable.settings

import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun SettingsRow(
    title: String,
    icon: ImageVector? = null,
    contentDescription: String? = title,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
