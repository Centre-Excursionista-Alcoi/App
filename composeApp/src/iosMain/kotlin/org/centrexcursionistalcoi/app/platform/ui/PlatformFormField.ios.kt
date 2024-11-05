package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.KeyboardType

@Composable
actual fun PlatformFormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier,
    enabled: Boolean,
    thisFocusRequester: FocusRequester?,
    nextFocusRequester: FocusRequester?,
    keyboardType: KeyboardType,
    isPassword: Boolean,
    error: String?,
    onSubmit: (() -> Unit)?
) {
}