package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.KeyboardType

@Composable
@OptIn(ExperimentalComposeUiApi::class)
expect fun PlatformFormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    thisFocusRequester: FocusRequester? = null,
    nextFocusRequester: FocusRequester? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    error: String? = null,
    supportingText: String? = null,
    autofillTypes: List<AutofillType>? = null,
    onSubmit: (() -> Unit)? = null
)
