package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.KeyboardType
import org.centrexcursionistalcoi.app.component.CarbonFormField

@Composable
@OptIn(ExperimentalComposeUiApi::class)
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
    supportingText: String?,
    autofillTypes: List<AutofillType>?,
    onSubmit: (() -> Unit)?
) {
    CarbonFormField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        enabled = enabled,
        thisFocusRequester = thisFocusRequester,
        nextFocusRequester = nextFocusRequester,
        keyboardType = keyboardType,
        isPassword = isPassword,
        error = error,
        autofillTypes = autofillTypes,
        supportingText = supportingText,
        onSubmit = onSubmit
    )
}
