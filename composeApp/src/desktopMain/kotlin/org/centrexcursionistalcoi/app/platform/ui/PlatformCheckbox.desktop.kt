package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gabrieldrn.carbon.checkbox.Checkbox
import com.gabrieldrn.carbon.common.selectable.SelectableInteractiveState

@Composable
actual fun PlatformCheckbox(
    checked: Boolean,
    onCheckedChanged: (Boolean) -> Unit,
    label: String?,
    modifier: Modifier,
    enabled: Boolean
) {
    Checkbox(
        checked = checked,
        onClick = { onCheckedChanged(!checked) },
        label = label ?: "",
        modifier = modifier,
        interactiveState = if (enabled) {
            SelectableInteractiveState.Default
        } else {
            SelectableInteractiveState.Disabled
        }
    )
}
