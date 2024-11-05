package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle

class PlatformTextStyles(
    val titleLarge: TextStyle,
    val titleRegular: TextStyle,
    val heading: TextStyle,
    val body: TextStyle,
    val label: TextStyle
)

@Composable
expect fun getPlatformTextStyles(): PlatformTextStyles
