package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
actual fun PlatformSettingsItem(
    title: String,
    modifier: Modifier,
    icon: ImageVector?,
    summary: String?,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = modifier.clickable(enabled = onClick != null) { onClick?.invoke() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(16.dp))
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                tint = getPlatformTextStyles().label.color
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            BasicText(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                style = getPlatformTextStyles().heading
            )
            summary?.let {
                BasicText(
                    text = it,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    style = getPlatformTextStyles().label
                )
            }
        }
        Spacer(Modifier.width(16.dp))
    }
}
