package ui.pages.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.login_action
import app.composeapp.generated.resources.login_email
import app.composeapp.generated.resources.login_lost
import app.composeapp.generated.resources.login_password
import app.composeapp.generated.resources.login_register
import app.composeapp.generated.resources.login_title
import org.jetbrains.compose.resources.stringResource
import ui.modifier.autofill
import ui.reusable.form.FormField

@Composable
@OptIn(ExperimentalComposeUiApi::class)
@Suppress("UnusedReceiverParameter")
fun ColumnScope.LoginPage(
    isLoading: Boolean,
    onLoginRequested: (email: String, password: String) -> Unit,
    onLostPassword: () -> Unit,
    onRegisterRequested: () -> Unit
) {
    Text(
        text = stringResource(Res.string.login_title),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        textAlign = TextAlign.Center
    )

    var email by remember { mutableStateOf(TextFieldValue("")) }
    var password by remember { mutableStateOf(TextFieldValue("")) }

    val passwordFocusRequester = remember { FocusRequester() }

    FormField(
        value = email,
        onValueChange = { email = it },
        label = stringResource(Res.string.login_email),
        enabled = !isLoading,
        keyboardType = KeyboardType.Email,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .autofill(AutofillType.EmailAddress) { email = TextFieldValue(it) },
        nextFocusRequester = passwordFocusRequester,
        onSubmit = { onLoginRequested(email.text, password.text) }
    )
    FormField(
        value = password,
        onValueChange = { password = it },
        label = stringResource(Res.string.login_password),
        enabled = !isLoading,
        isPassword = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .autofill(AutofillType.Password) { password = TextFieldValue(it) }
            .focusRequester(passwordFocusRequester),
        onSubmit = { onLoginRequested(email.text, password.text) }
    )

    Spacer(Modifier.height(16.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onLostPassword,
            enabled = !isLoading
        ) {
            Text(stringResource(Res.string.login_lost))
        }
        OutlinedButton(
            onClick = { onLoginRequested(email.text, password.text) },
            enabled = !isLoading
        ) {
            Text(stringResource(Res.string.login_action))
        }
    }

    Text(
        text = stringResource(Res.string.login_register),
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier
            .clickable(enabled = !isLoading, onClick = onRegisterRequested)
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 16.dp),
        textAlign = TextAlign.Center
    )
}
