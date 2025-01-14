package org.centrexcursionistalcoi.app.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ceaapp.composeapp.generated.resources.*
import com.gabrieldrn.carbon.foundation.color.CarbonLayer
import com.gabrieldrn.carbon.foundation.color.containerBackground
import org.centrexcursionistalcoi.app.component.AppText
import org.centrexcursionistalcoi.app.composition.LocalNavController
import org.centrexcursionistalcoi.app.platform.ui.PlatformButton
import org.centrexcursionistalcoi.app.platform.ui.PlatformFormField
import org.centrexcursionistalcoi.app.platform.ui.getPlatformTextStyles
import org.centrexcursionistalcoi.app.route.Login
import org.centrexcursionistalcoi.app.route.Register
import org.centrexcursionistalcoi.app.validation.isValidEmail
import org.centrexcursionistalcoi.app.validation.isValidNif
import org.centrexcursionistalcoi.app.viewmodel.RegisterViewModel
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalComposeUiApi::class)
object RegisterScreen : Screen<Register, RegisterViewModel>(::RegisterViewModel) {
    @Composable
    override fun Content(viewModel: RegisterViewModel) {
        val navController = LocalNavController.current

        val isLoading by viewModel.isLoading.collectAsState()
        val error by viewModel.error.collectAsState()

        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var passwordConfirm by remember { mutableStateOf("") }
        var firstName by remember { mutableStateOf("") }
        var familyName by remember { mutableStateOf("") }
        var nif by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }

        val emailFocusRequester = remember { FocusRequester() }
        val passwordFocusRequester = remember { FocusRequester() }
        val passwordConfirmFocusRequester = remember { FocusRequester() }
        val firstNameFocusRequester = remember { FocusRequester() }
        val familyNameFocusRequester = remember { FocusRequester() }
        val nifFocusRequester = remember { FocusRequester() }
        val phoneFocusRequester = remember { FocusRequester() }

        val fieldsValid = email.isValidEmail &&
                password.isNotBlank() && password == passwordConfirm &&
                firstName.isNotBlank() &&
                familyName.isNotBlank() &&
                nif.isNotBlank() && nif.isValidNif &&
                phone.isNotBlank()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            AppText(
                text = stringResource(Res.string.register_title),
                style = getPlatformTextStyles().titleLarge,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
            )

            PlatformFormField(
                value = email,
                onValueChange = { email = it; viewModel.clearError() },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                label = stringResource(Res.string.register_email),
                error = if (email.isNotBlank() && !email.isValidEmail) stringResource(Res.string.error_email_invalid) else null,
                thisFocusRequester = emailFocusRequester,
                nextFocusRequester = passwordFocusRequester,
                autofillTypes = listOf(AutofillType.EmailAddress)
            )
            PlatformFormField(
                value = password,
                onValueChange = { password = it },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                label = stringResource(Res.string.register_password),
                isPassword = true,
                thisFocusRequester = passwordFocusRequester,
                nextFocusRequester = passwordConfirmFocusRequester,
                autofillTypes = listOf(AutofillType.Password, AutofillType.NewPassword)
            )
            PlatformFormField(
                value = passwordConfirm,
                onValueChange = { passwordConfirm = it; viewModel.clearError() },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                label = stringResource(Res.string.register_password_confirm),
                isPassword = true,
                error = if (passwordConfirm.isNotBlank() && password != passwordConfirm) stringResource(Res.string.error_passwords_dont_match) else null,
                thisFocusRequester = passwordConfirmFocusRequester,
                nextFocusRequester = firstNameFocusRequester,
                autofillTypes = listOf(AutofillType.Password, AutofillType.NewPassword)
            )
            PlatformFormField(
                value = firstName,
                onValueChange = { firstName = it; viewModel.clearError() },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                label = stringResource(Res.string.register_first_name),
                thisFocusRequester = firstNameFocusRequester,
                nextFocusRequester = familyNameFocusRequester,
                autofillTypes = listOf(AutofillType.PersonFirstName, AutofillType.PersonNamePrefix)
            )
            PlatformFormField(
                value = familyName,
                onValueChange = { familyName = it; viewModel.clearError() },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                label = stringResource(Res.string.register_family_name),
                thisFocusRequester = familyNameFocusRequester,
                nextFocusRequester = nifFocusRequester,
                autofillTypes = listOf(AutofillType.PersonLastName, AutofillType.PersonMiddleName, AutofillType.PersonNameSuffix)
            )
            PlatformFormField(
                value = nif,
                onValueChange = { nif = it.uppercase(); viewModel.clearError() },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                label = stringResource(Res.string.register_nif),
                thisFocusRequester = nifFocusRequester,
                nextFocusRequester = phoneFocusRequester,
                error = if (nif.isNotBlank() && !nif.isValidNif) stringResource(Res.string.error_nif_invalid) else null
            )
            PlatformFormField(
                value = phone,
                onValueChange = { phone = it; viewModel.clearError() },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                label = stringResource(Res.string.register_phone),
                thisFocusRequester = phoneFocusRequester,
                autofillTypes = listOf(AutofillType.PhoneNumber)
            )

            AnimatedContent(
                targetState = error
            ) { err ->
                if (err != null) {
                    CarbonLayer {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .containerBackground()
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            AppText(
                                text = err,
                                style = getPlatformTextStyles().body,
                                color = { Color.Red },
                                modifier = Modifier.fillMaxWidth().padding(8.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppText(
                    text = stringResource(Res.string.register_already),
                    style = getPlatformTextStyles().label,
                    modifier = Modifier
                        .clickable {
                            navController.navigate(Login)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
                PlatformButton(
                    text = stringResource(Res.string.register_button),
                    enabled = fieldsValid && !isLoading
                ) {
                    viewModel.register(navController, email, password, firstName, familyName, nif, phone)
                }
            }
        }
    }
}
