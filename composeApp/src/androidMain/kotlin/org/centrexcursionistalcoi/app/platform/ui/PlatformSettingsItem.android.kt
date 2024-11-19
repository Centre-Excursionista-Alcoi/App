package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
actual fun PlatformSettingsItem(
    title: String,
    modifier: Modifier,
    icon: ImageVector?,
    summary: String?,
    onClick: (() -> Unit)?
) {
    ListItem(
        headlineContent = { Text(title) },
        modifier = modifier.clickable(enabled = onClick != null) { onClick?.invoke() },
        supportingContent = if (summary != null) { { Text(summary) } } else { null },
        leadingContent = if (icon != null) { { Icon(icon, summary) } } else { null }
    )
}
