package org.centrexcursionistalcoi.app.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.unit.dp
import org.centrexcursionistalcoi.app.composition.AccountStateNavigator
import org.centrexcursionistalcoi.app.platform.ui.PlatformButton
import org.centrexcursionistalcoi.app.platform.ui.PlatformFormField
import org.centrexcursionistalcoi.app.route.Login
import org.centrexcursionistalcoi.app.validation.isValidEmail
import org.centrexcursionistalcoi.app.viewmodel.LoginViewModel

object LoginScreen : Screen<Login, LoginViewModel>(::LoginViewModel) {
    @Composable
    override fun Content(viewModel: LoginViewModel) {
        val isLoading by viewModel.isLoading.collectAsState()
        val error by viewModel.error.collectAsState()

        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }

        val emailFocusRequester = remember { FocusRequester() }
        val passwordFocusRequester = remember { FocusRequester() }

        val fieldsValid = remember(email, password) { email.isNotBlank() && email.isValidEmail && password.isNotBlank() }

        AccountStateNavigator(onLoggedIn = LoadingScreen)

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)
        ) {
            PlatformFormField(
                value = email,
                onValueChange = { email = it; viewModel.clearError() },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusProperties {
                        next = passwordFocusRequester
                        right = passwordFocusRequester
                        down = passwordFocusRequester
                    },
                label = "Email",
                thisFocusRequester = emailFocusRequester,
                nextFocusRequester = passwordFocusRequester,
                error = error?.let { "" }, // Do not show any message, just show in red
                onSubmit = { if (fieldsValid) viewModel.login(email, password) }
            )
            PlatformFormField(
                value = password,
                onValueChange = { password = it; viewModel.clearError() },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .focusProperties {
                        previous = emailFocusRequester
                        left = emailFocusRequester
                        up = emailFocusRequester
                    },
                label = "Password",
                thisFocusRequester = passwordFocusRequester,
                isPassword = true,
                error = error,
                onSubmit = { if (fieldsValid) viewModel.login(email, password) }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                PlatformButton(
                    text = "Login",
                    enabled = !isLoading && fieldsValid,
                ) {
                    viewModel.login(email, password)
                }
            }
        }
    }
}