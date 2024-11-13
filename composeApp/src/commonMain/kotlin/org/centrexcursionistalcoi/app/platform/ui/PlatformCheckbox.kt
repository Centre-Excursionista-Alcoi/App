package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PlatformCheckbox(
    checked: Boolean,
    onCheckedChanged: (Boolean) -> Unit,
    label: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
)
