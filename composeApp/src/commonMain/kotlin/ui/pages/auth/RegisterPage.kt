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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.datetime.LocalDate
import resources.MR
import ui.modifier.autofill
import ui.reusable.form.FieldFormatValidators
import ui.reusable.form.FormDatePicker
import ui.reusable.form.FormField

@Composable
@OptIn(ExperimentalComposeUiApi::class)
@Suppress("UnusedReceiverParameter")
fun ColumnScope.RegisterPage(
    isLoading: Boolean,
    onLoginRequested: () -> Unit,
    onRegisterRequested: (email: String, password: String, fullName: String, phone: String, city: String, birthday: LocalDate) -> Unit
) {
    Text(
        text = stringResource(MR.strings.register_title),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        textAlign = TextAlign.Center
    )

    var email by remember { mutableStateOf<TextFieldValue?>(null) }
    var password by remember { mutableStateOf<TextFieldValue?>(null) }
    var fullName by remember { mutableStateOf<TextFieldValue?>(null) }
    var phone by remember { mutableStateOf<TextFieldValue?>(null) }
    var city by remember { mutableStateOf<TextFieldValue?>(null) }
    var birthday by remember { mutableStateOf<LocalDate?>(null) }

    val formFilled = !email?.text.isNullOrBlank() &&
        !password?.text.isNullOrBlank() &&
        !fullName?.text.isNullOrBlank() &&
        !phone?.text.isNullOrBlank() &&
        !city?.text.isNullOrBlank() &&
        birthday != null

    val register: () -> Unit = {
        if (formFilled) {
            onRegisterRequested(
                email!!.text,
                password!!.text,
                fullName!!.text,
                phone!!.text,
                city!!.text,
                birthday!!
            )
        }
    }

    val passwordFocusRequester = remember { FocusRequester() }
    val fullNameFocusRequester = remember { FocusRequester() }
    val phoneFocusRequester = remember { FocusRequester() }
    val cityFocusRequester = remember { FocusRequester() }

    FormField(
        value = email,
        onValueChange = { email = it },
        label = stringResource(MR.strings.register_email),
        enabled = !isLoading,
        isRequired = true,
        keyboardType = KeyboardType.Email,
        validator = FieldFormatValidators.Email,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .autofill(AutofillType.EmailAddress) { email = TextFieldValue(it) },
        nextFocusRequester = passwordFocusRequester,
        onSubmit = register
    )
    FormField(
        value = password,
        onValueChange = { password = it },
        label = stringResource(MR.strings.register_password),
        enabled = !isLoading,
        isRequired = true,
        isPassword = true,
        validator = FieldFormatValidators.Password,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .autofill(AutofillType.Password) { password = TextFieldValue(it) }
            .focusRequester(passwordFocusRequester),
        nextFocusRequester = fullNameFocusRequester,
        onSubmit = register
    )
    FormField(
        value = fullName,
        onValueChange = { fullName = it },
        label = stringResource(MR.strings.register_name),
        enabled = !isLoading,
        isRequired = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .autofill(AutofillType.PersonFullName) { fullName = TextFieldValue(it) }
            .focusRequester(fullNameFocusRequester),
        nextFocusRequester = phoneFocusRequester,
        onSubmit = register
    )
    FormField(
        value = phone,
        onValueChange = { phone = it },
        label = stringResource(MR.strings.register_phone),
        enabled = !isLoading,
        isRequired = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .autofill(AutofillType.PhoneNumber) { phone = TextFieldValue(it) }
            .focusRequester(phoneFocusRequester),
        nextFocusRequester = cityFocusRequester,
        onSubmit = register
    )
    FormField(
        value = city,
        onValueChange = { city = it },
        label = stringResource(MR.strings.register_city),
        enabled = !isLoading,
        isRequired = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .autofill(AutofillType.AddressLocality) { city = TextFieldValue(it) }
            .focusRequester(cityFocusRequester),
        capitalization = KeyboardCapitalization.Words,
        onSubmit = register
    )
    FormDatePicker(
        value = birthday,
        onValueChange = { birthday = it },
        label = stringResource(MR.strings.register_birthday),
        enabled = !isLoading,
        isRequired = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .autofill(AutofillType.BirthDateFull) {
                try {
                    birthday = LocalDate.parse(it)
                } catch (_: IllegalArgumentException) {
                    // ignore
                }
            }
    )

    Spacer(Modifier.height(16.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = register,
            enabled = !isLoading && formFilled
        ) {
            Text(stringResource(MR.strings.register_action))
        }
    }

    Text(
        text = stringResource(MR.strings.register_login),
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 16.dp)
            .clickable(enabled = !isLoading, onClick = onLoginRequested),
        textAlign = TextAlign.Center
    )
}
