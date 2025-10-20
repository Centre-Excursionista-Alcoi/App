package org.centrexcursionistalcoi.app.ui.reusable.form

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import cea_app.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun PasswordFormField(
    state: TextFieldState,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    semanticsIsNewPassword: Boolean = false,
    showNextButton: Boolean = false,
) {
    var displayingPassword by remember { mutableStateOf(false) }

    OutlinedSecureTextField(
        state = state,
        enabled = enabled,
        label = { Text(label) },
        modifier = modifier
            .semantics {
                contentType = if (semanticsIsNewPassword) ContentType.NewPassword else ContentType.Password
            },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = if (showNextButton) ImeAction.Next else ImeAction.Done),
        trailingIcon = {
            IconButton(
                onClick = { displayingPassword = !displayingPassword }
            ) {
                Icon(
                    if (displayingPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    stringResource(if (displayingPassword) Res.string.password_hide else Res.string.password_show)
                )
            }
        },
        textObfuscationMode = if (displayingPassword) {
            TextObfuscationMode.Visible
        } else {
            TextObfuscationMode.RevealLastTyped
        },
    )
}
