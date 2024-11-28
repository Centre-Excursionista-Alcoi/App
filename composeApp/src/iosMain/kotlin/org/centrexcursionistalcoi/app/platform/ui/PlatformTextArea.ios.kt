package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.CupertinoTextField

@Composable
actual fun PlatformTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier,
    enabled: Boolean
) {
    CupertinoTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        placeholder = { CupertinoText(label) },
        modifier = modifier
    )
}
