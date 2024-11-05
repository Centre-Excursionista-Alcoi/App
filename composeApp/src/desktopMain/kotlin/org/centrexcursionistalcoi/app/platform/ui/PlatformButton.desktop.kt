package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.centrexcursionistalcoi.app.component.CarbonButton

@Composable
actual fun PlatformButton(
    text: String,
    modifier: Modifier,
    enabled: Boolean,
    onClick: () -> Unit
) {
    CarbonButton(
        text = text,
        modifier = modifier,
        enabled = enabled,
        onClick = onClick
    )
}
