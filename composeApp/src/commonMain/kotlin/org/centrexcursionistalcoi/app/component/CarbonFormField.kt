package org.centrexcursionistalcoi.app.component

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.gabrieldrn.carbon.textinput.PasswordInput
import com.gabrieldrn.carbon.textinput.TextInput
import com.gabrieldrn.carbon.textinput.TextInputState

@Composable
fun CarbonFormField(
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
    onSubmit: (() -> Unit)? = null
) {
    val keyboard = LocalSoftwareKeyboardController.current

    if (isPassword) {
        var passwordHidden by remember { mutableStateOf(true) }

        PasswordInput(
            label = label,
            value = value,
            onValueChange = onValueChange,
            passwordHidden = passwordHidden,
            onPasswordHiddenChange = { passwordHidden = it },
            modifier = Modifier
                .let { mod ->
                    thisFocusRequester?.let { mod.focusRequester(it) } ?: mod
                }
                .then(modifier),
            state = if (enabled) if (error != null) TextInputState.Error else TextInputState.Enabled else TextInputState.Disabled,
            helperText = error ?: "",
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = false,
                imeAction = if (nextFocusRequester != null) {
                    ImeAction.Next
                } else if (onSubmit != null) {
                    ImeAction.Go
                } else {
                    ImeAction.Done
                },
                keyboardType = if (isPassword) KeyboardType.Password else keyboardType
            ),
            keyboardActions = KeyboardActions {
                if (nextFocusRequester != null) {
                    nextFocusRequester.requestFocus()
                } else if (onSubmit != null) {
                    onSubmit()
                } else {
                    keyboard?.hide()
                }
            }
        )
    } else {
        TextInput(
            label = label,
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .let { mod ->
                    thisFocusRequester?.let { mod.focusRequester(it) } ?: mod
                }
                .then(modifier),
            state = if (enabled) if (error != null) TextInputState.Error else TextInputState.Enabled else TextInputState.Disabled,
            helperText = error ?: "",
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = false,
                imeAction = if (nextFocusRequester != null) {
                    ImeAction.Next
                } else if (onSubmit != null) {
                    ImeAction.Go
                } else {
                    ImeAction.Done
                },
                keyboardType = if (isPassword) KeyboardType.Password else keyboardType
            ),
            keyboardActions = KeyboardActions {
                if (nextFocusRequester != null) {
                    nextFocusRequester.requestFocus()
                } else if (onSubmit != null) {
                    onSubmit()
                } else {
                    keyboard?.hide()
                }
            }
        )
    }
}
