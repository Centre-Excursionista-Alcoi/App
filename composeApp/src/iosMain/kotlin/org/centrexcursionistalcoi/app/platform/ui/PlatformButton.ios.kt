package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.alexzhirkevich.cupertino.CupertinoButton
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.ExperimentalCupertinoApi

@OptIn(ExperimentalCupertinoApi::class)
@Composable
actual fun PlatformButton(
    text: String,
    modifier: Modifier,
    enabled: Boolean,
    onClick: () -> Unit
) {
    CupertinoButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
    ) { CupertinoText(text) }
}
