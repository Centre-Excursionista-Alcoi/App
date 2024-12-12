package org.centrexcursionistalcoi.app.component

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.gabrieldrn.carbon.textinput.PasswordInput
import com.gabrieldrn.carbon.textinput.TextInput
import com.gabrieldrn.carbon.textinput.TextInputState
import org.centrexcursionistalcoi.app.modifier.autofill
import org.centrexcursionistalcoi.app.utils.applyIf
import org.centrexcursionistalcoi.app.utils.applyIfNotNull

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CarbonFormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    thisFocusRequester: FocusRequester? = null,
    previousFocusRequester: FocusRequester? = null,
    nextFocusRequester: FocusRequester? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    error: String? = null,
    supportingText: String? = null,
    autofillTypes: List<AutofillType>? = null,
    onSubmit: (() -> Unit)? = null
) {
    val keyboard = LocalSoftwareKeyboardController.current

    val mod = Modifier
        .applyIfNotNull(thisFocusRequester, Modifier::focusRequester)
        .applyIfNotNull(previousFocusRequester, nextFocusRequester, condition = Boolean::or) { (pfr, nfr) ->
            focusProperties {
                pfr?.let {
                    previous = it
                    left = it
                    up = it
                }
                nfr?.let {
                    next = it
                    right = it
                    down = it
                }
            }
        }
        .applyIf({ !autofillTypes.isNullOrEmpty() }) {
            autofill(autofillTypes!!) { onValueChange(it) }
        }
        .then(modifier)
    val imeAction = if (nextFocusRequester != null) {
        ImeAction.Next
    } else if (onSubmit != null) {
        ImeAction.Go
    } else {
        ImeAction.Done
    }
    val keyboardActions = KeyboardActions {
        if (nextFocusRequester != null) {
            nextFocusRequester.requestFocus()
        } else if (onSubmit != null) {
            onSubmit()
        } else {
            keyboard?.hide()
        }
    }

    if (isPassword) {
        var passwordHidden by remember { mutableStateOf(true) }

        PasswordInput(
            label = label,
            value = value,
            onValueChange = onValueChange,
            passwordHidden = passwordHidden,
            onPasswordHiddenChange = { passwordHidden = it },
            modifier = mod,
            state = if (enabled) if (error != null) TextInputState.Error else TextInputState.Enabled else TextInputState.Disabled,
            helperText = error ?: supportingText ?: "",
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = false,
                imeAction = imeAction,
                keyboardType = if (isPassword) KeyboardType.Password else keyboardType
            ),
            keyboardActions = keyboardActions
        )
    } else {
        TextInput(
            label = label,
            value = value,
            onValueChange = onValueChange,
            modifier = mod,
            state = if (enabled) if (error != null) TextInputState.Error else TextInputState.Enabled else TextInputState.Disabled,
            helperText = error ?: supportingText ?: "",
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = false,
                imeAction = imeAction,
                keyboardType = keyboardType
            ),
            keyboardActions = keyboardActions
        )
    }
}
