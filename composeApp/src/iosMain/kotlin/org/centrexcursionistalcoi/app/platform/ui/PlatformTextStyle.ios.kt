package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.runtime.Composable
import io.github.alexzhirkevich.LocalContentColor
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme

@Composable
actual fun getPlatformTextStyles(): PlatformTextStyles {
    val contentColor = LocalContentColor.current
    return PlatformTextStyles(
        titleLarge = CupertinoTheme.typography.headline.copy(color = contentColor),
        titleRegular = CupertinoTheme.typography.largeTitle.copy(color = contentColor),
        heading = CupertinoTheme.typography.title1.copy(color = contentColor),
        body = CupertinoTheme.typography.body.copy(color = contentColor),
        label = CupertinoTheme.typography.caption1.copy(color = contentColor)
    )
}
