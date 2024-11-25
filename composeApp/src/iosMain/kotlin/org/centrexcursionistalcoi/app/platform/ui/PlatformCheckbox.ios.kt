package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.alexzhirkevich.cupertino.CupertinoCheckBox
import io.github.alexzhirkevich.cupertino.CupertinoText

@Composable
actual fun PlatformCheckbox(
    checked: Boolean,
    onCheckedChanged: (Boolean) -> Unit,
    label: String?,
    modifier: Modifier,
    enabled: Boolean
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CupertinoCheckBox(checked, onCheckedChanged, enabled = enabled)
        label?.let { CupertinoText(it, modifier = Modifier.weight(1f)) }
    }
}
