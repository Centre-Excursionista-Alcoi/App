package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
actual fun getPlatformTextStyles(): PlatformTextStyles {
    val contentColor = LocalContentColor.current
    return PlatformTextStyles(
        titleLarge = MaterialTheme.typography.headlineLarge.copy(color = contentColor),
        titleRegular = MaterialTheme.typography.headlineSmall.copy(color = contentColor),
        heading = MaterialTheme.typography.titleSmall.copy(color = contentColor),
        body = MaterialTheme.typography.bodyMedium.copy(color = contentColor),
        label = MaterialTheme.typography.labelMedium.copy(color = contentColor)
    )
}
