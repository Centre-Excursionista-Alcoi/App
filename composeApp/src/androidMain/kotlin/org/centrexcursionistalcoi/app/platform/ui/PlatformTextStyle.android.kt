package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
actual fun getPlatformTextStyles(): PlatformTextStyles {
    return PlatformTextStyles(
        titleLarge = MaterialTheme.typography.headlineLarge,
        titleRegular = MaterialTheme.typography.headlineSmall,
        heading = MaterialTheme.typography.titleSmall,
        body = MaterialTheme.typography.bodyMedium,
        label = MaterialTheme.typography.labelMedium
    )
}
