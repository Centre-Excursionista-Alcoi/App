package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import ceaapp.composeapp.generated.resources.Res
import ceaapp.composeapp.generated.resources.hide_password
import ceaapp.composeapp.generated.resources.show_password
import org.jetbrains.compose.resources.stringResource

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
    val softwareKeyboard = LocalSoftwareKeyboardController.current
    var textVisible by remember { mutableStateOf(!isPassword) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.let { mod ->
            if (thisFocusRequester != null)
                mod.focusRequester(thisFocusRequester)
            else
                mod
        },
        enabled = enabled,
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        visualTransformation = if (textVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            imeAction = if (onSubmit != null) ImeAction.Go else if (nextFocusRequester != null) ImeAction.Next else ImeAction.Done,
            keyboardType = keyboardType
        ),
        keyboardActions = KeyboardActions {
            if (onSubmit != null) {
                onSubmit()
            } else if (nextFocusRequester != null) {
                nextFocusRequester.requestFocus()
            } else {
                softwareKeyboard?.hide()
            }
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(
                    onClick = { textVisible = !textVisible }
                ) {
                    Icon(
                        imageVector = if (textVisible) {
                            Icons.Default.Visibility
                        } else {
                            Icons.Default.VisibilityOff
                        },
                        contentDescription = if (textVisible) {
                            stringResource(Res.string.hide_password)
                        } else {
                            stringResource(Res.string.show_password)
                        }
                    )
                }
            }
        } else null
    )
}
