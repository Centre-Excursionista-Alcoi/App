package org.centrexcursionistalcoi.app.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.unit.dp
import ceaapp.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.composition.LocalNavController
import org.centrexcursionistalcoi.app.modifier.autofill
import org.centrexcursionistalcoi.app.platform.ui.PlatformButton
import org.centrexcursionistalcoi.app.platform.ui.PlatformFormField
import org.centrexcursionistalcoi.app.platform.ui.getPlatformTextStyles
import org.centrexcursionistalcoi.app.route.Login
import org.centrexcursionistalcoi.app.route.Register
import org.centrexcursionistalcoi.app.validation.isValidEmail
import org.centrexcursionistalcoi.app.viewmodel.LoginViewModel
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalComposeUiApi::class)
object LoginScreen : Screen<Login, LoginViewModel>(::LoginViewModel) {
    @Composable
    override fun Content(viewModel: LoginViewModel) {
        val navController = LocalNavController.current

        val isLoading by viewModel.isLoading.collectAsState()
        val error by viewModel.error.collectAsState()

        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }

        val emailFocusRequester = remember { FocusRequester() }
        val passwordFocusRequester = remember { FocusRequester() }

        val fieldsValid =
            remember(email, password) { email.isNotBlank() && email.isValidEmail && password.isNotBlank() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            BasicText(
                text = stringResource(Res.string.login_title),
                style = getPlatformTextStyles().titleLarge,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
            )

            PlatformFormField(
                value = email,
                onValueChange = { email = it; viewModel.clearError() },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .focusProperties {
                        next = passwordFocusRequester
                        right = passwordFocusRequester
                        down = passwordFocusRequester
                    }
                    .autofill(
                        listOf(AutofillType.EmailAddress)
                    ) { email = it; viewModel.clearError() },
                label = stringResource(Res.string.login_email),
                thisFocusRequester = emailFocusRequester,
                nextFocusRequester = passwordFocusRequester,
                error = error?.let { "" }, // Do not show any message, just show in red
                onSubmit = { if (fieldsValid) viewModel.login(navController, email, password) }
            )
            PlatformFormField(
                value = password,
                onValueChange = { password = it; viewModel.clearError() },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .focusProperties {
                        previous = emailFocusRequester
                        left = emailFocusRequester
                        up = emailFocusRequester
                    }
                    .autofill(
                        listOf(AutofillType.Password)
                    ) { password = it; viewModel.clearError() },
                label = stringResource(Res.string.login_password),
                thisFocusRequester = passwordFocusRequester,
                isPassword = true,
                error = error,
                onSubmit = { if (fieldsValid) viewModel.login(navController, email, password) }
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    text = stringResource(Res.string.login_no_account),
                    style = getPlatformTextStyles().label,
                    modifier = Modifier
                        .clickable {
                            navController.navigate(Register)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
                PlatformButton(
                    text = stringResource(Res.string.login_button),
                    enabled = !isLoading && fieldsValid,
                ) {
                    viewModel.login(navController, email, password)
                }
            }
        }
    }
}