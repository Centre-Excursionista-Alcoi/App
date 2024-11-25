package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.runtime.Composable
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme

@Composable
actual fun PlatformTheme(content: @Composable () -> Unit) {
    CupertinoTheme(
        content = content
    )
}
