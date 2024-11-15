package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
actual fun PlatformSettingsItem(
    title: String,
    icon: ImageVector,
    modifier: Modifier,
    summary: String?,
    onClick: (() -> Unit)?
) {
    ListItem(
        headlineContent = { Text(title) },
        modifier = modifier,
        supportingContent = if (summary != null) { { Text(summary) } } else { null },
        leadingContent = { Icon(icon, summary) }
    )
}
