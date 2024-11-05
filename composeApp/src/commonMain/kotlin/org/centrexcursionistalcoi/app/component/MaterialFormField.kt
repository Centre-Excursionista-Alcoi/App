package org.centrexcursionistalcoi.app.component

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun MaterialFormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    thisFocusRequester: FocusRequester? = null,
    nextFocusRequester: FocusRequester? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    val keyboard = LocalSoftwareKeyboardController.current

    var displayingPassword by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .let { mod ->
                thisFocusRequester?.let { mod.focusRequester(it) } ?: mod
            }
            .then(modifier),
        trailingIcon = if (isPassword) {
            {
                val icon = if (displayingPassword) {
                    Icons.Filled.VisibilityOff
                } else {
                    Icons.Filled.Visibility
                }
                IconButton(onClick = { displayingPassword = !displayingPassword }) {
                    Icon(icon, contentDescription = null)
                }
            }
        } else {
            null
        },
        singleLine = true,
        visualTransformation = if (isPassword && !displayingPassword) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(
            autoCorrectEnabled = false,
            imeAction = if (nextFocusRequester != null) ImeAction.Next else ImeAction.Done,
            keyboardType = if (isPassword) KeyboardType.Password else keyboardType
        ),
        keyboardActions = KeyboardActions {
            if (nextFocusRequester != null) {
                nextFocusRequester.requestFocus()
            } else {
                keyboard?.hide()
            }
        }
    )
}
