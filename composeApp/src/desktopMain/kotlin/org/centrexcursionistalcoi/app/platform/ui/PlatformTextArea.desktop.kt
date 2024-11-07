package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gabrieldrn.carbon.textinput.TextArea
import com.gabrieldrn.carbon.textinput.TextInputState

@Composable
actual fun PlatformTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier,
    enabled: Boolean
) {
    TextArea(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        state = if (enabled) TextInputState.Enabled else TextInputState.Disabled
    )
}
