package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.banner
import cea_app.composeapp.generated.resources.cancel
import cea_app.composeapp.generated.resources.close
import cea_app.composeapp.generated.resources.confirm_password
import cea_app.composeapp.generated.resources.email
import cea_app.composeapp.generated.resources.login_action
import cea_app.composeapp.generated.resources.login_error_invalid_credentials
import cea_app.composeapp.generated.resources.login_error_passkey
import cea_app.composeapp.generated.resources.login_error_password_not_set
import cea_app.composeapp.generated.resources.login_error_password_not_set_desktop
import cea_app.composeapp.generated.resources.login_error_unknown
import cea_app.composeapp.generated.resources.login_error_user_not_registered
import cea_app.composeapp.generated.resources.login_existing_account_logout
import cea_app.composeapp.generated.resources.login_existing_account_message
import cea_app.composeapp.generated.resources.login_existing_account_title
import cea_app.composeapp.generated.resources.login_forgot_password
import cea_app.composeapp.generated.resources.login_forgot_password_dialog_action
import cea_app.composeapp.generated.resources.login_forgot_password_dialog_message
import cea_app.composeapp.generated.resources.login_forgot_password_dialog_title
import cea_app.composeapp.generated.resources.login_forgot_password_success_title
import cea_app.composeapp.generated.resources.login_or_type
import cea_app.composeapp.generated.resources.login_password_changed_message
import cea_app.composeapp.generated.resources.login_password_changed_title
import cea_app.composeapp.generated.resources.login_saved_credential_action
import cea_app.composeapp.generated.resources.password
import cea_app.composeapp.generated.resources.people
import cea_app.composeapp.generated.resources.register_action
import cea_app.composeapp.generated.resources.register_back
import cea_app.composeapp.generated.resources.register_code
import cea_app.composeapp.generated.resources.register_code_message
import cea_app.composeapp.generated.resources.register_code_resend
import cea_app.composeapp.generated.resources.register_continue
import cea_app.composeapp.generated.resources.register_email_message
import cea_app.composeapp.generated.resources.register_error_already_registered
import cea_app.composeapp.generated.resources.register_error_email_not_found
import cea_app.composeapp.generated.resources.register_error_invalid_code
import cea_app.composeapp.generated.resources.register_error_member_not_active
import cea_app.composeapp.generated.resources.register_error_password_not_safe
import cea_app.composeapp.generated.resources.register_passkey_instead
import cea_app.composeapp.generated.resources.register_password_instead
import cea_app.composeapp.generated.resources.register_password_message
import cea_app.composeapp.generated.resources.register_title
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.auth.PasskeyException
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Error
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Security
import org.centrexcursionistalcoi.app.ui.resources.GenderedStringResource
import org.centrexcursionistalcoi.app.ui.reusable.ColumnWidthWrapper
import org.centrexcursionistalcoi.app.ui.reusable.PasskeyExplanationCard
import org.centrexcursionistalcoi.app.ui.reusable.form.PasswordFormField
import org.centrexcursionistalcoi.app.ui.utils.unknown
import org.centrexcursionistalcoi.app.viewmodel.LoginViewModel
import org.centrexcursionistalcoi.app.viewmodel.RegistrationStep
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AuthScreen(
    changedPassword: Boolean = false,
    model: LoginViewModel = koinViewModel(),
    onLoginSuccess: () -> Unit,
) {
    val isLoading by model.isLoading.collectAsState()
    val error by model.error.collectAsState()
    val existingAccountEmail by model.existingAccountEmail.collectAsState()
    val registrationStep by model.registrationStep.collectAsState()

    // Tells the platform's autofill the form was submitted successfully, so it offers to save the password.
    val autofillManager = LocalAutofillManager.current
    val afterLogin: () -> Unit = {
        autofillManager?.commit()
        onLoginSuccess()
    }

    AuthScreen(
        isLoading = isLoading,
        error = error,
        changedPassword = changedPassword,
        existingAccountEmail = existingAccountEmail,
        canUseSavedCredentials = model.canUseSavedCredentials,
        onForgetExistingAccount = model::forgetExistingAccount,
        onSavedCredentialRequest = { model.signInWithSavedCredential(onLoginSuccess) },
        onLoginRequest = { email, password -> model.login(email, password, afterLogin = afterLogin) },
        registrationStep = registrationStep,
        canRegisterWithPasskey = model.canRegisterWithPasskey,
        onRegistrationCodeRequest = model::requestRegistrationCode,
        onRegistrationCodeEntered = model::enterRegistrationCode,
        onRegistrationBack = model::registrationBack,
        onRegisterWithPasskeyRequest = { model.registerWithPasskey(onLoginSuccess) },
        onRegisterRequest = { password -> model.register(password, afterLogin) },
        onForgotPassword = { email, ar -> model.forgotPassword(email, ar) },
        onClearErrors = model::clearError,
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
private fun AuthScreen(
    isLoading: Boolean,
    error: Throwable?,
    changedPassword: Boolean,
    existingAccountEmail: String? = null,
    canUseSavedCredentials: Boolean = false,
    onForgetExistingAccount: () -> Unit = {},
    onSavedCredentialRequest: () -> Unit = {},
    onLoginRequest: (email: String, password: String) -> Unit,
    registrationStep: RegistrationStep = RegistrationStep.Email,
    canRegisterWithPasskey: Boolean = false,
    onRegistrationCodeRequest: (email: String) -> Unit = {},
    onRegistrationCodeEntered: (code: String) -> Unit = {},
    onRegistrationBack: () -> Unit = {},
    onRegisterWithPasskeyRequest: () -> Unit = {},
    onRegisterRequest: (password: String) -> Unit,
    onForgotPassword: (email: String, afterRequest: () -> Unit) -> Job,
    onClearErrors: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val state = rememberPagerState { 2 }
    val snackbarHostState = remember { SnackbarHostState() }

    var showingChangedPasswordDialog by remember { mutableStateOf(changedPassword) }
    if (showingChangedPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showingChangedPasswordDialog = false },
            title = { Text(stringResource(Res.string.login_password_changed_title)) },
            text = { Text(stringResource(Res.string.login_password_changed_message)) },
            confirmButton = {
                TextButton(onClick = { showingChangedPasswordDialog = false }) {
                    Text(stringResource(Res.string.close))
                }
            }
        )
    }

    var dismissedExistingAccountWarning by remember { mutableStateOf(false) }
    if (existingAccountEmail != null && !dismissedExistingAccountWarning) {
        AlertDialog(
            onDismissRequest = { dismissedExistingAccountWarning = true },
            title = { Text(stringResource(Res.string.login_existing_account_title)) },
            text = { Text(stringResource(Res.string.login_existing_account_message, existingAccountEmail)) },
            confirmButton = {
                TextButton(onClick = {
                    onForgetExistingAccount()
                    dismissedExistingAccountWarning = true
                }) {
                    Text(stringResource(Res.string.login_existing_account_logout))
                }
            },
            dismissButton = {
                TextButton(onClick = { dismissedExistingAccountWarning = true }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }

    Scaffold { paddingValues ->
        HorizontalPager(
            state = state,
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            userScrollEnabled = false,
            key = { it },
        ) { page ->
            ColumnWidthWrapper(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (page) {
                    0 -> AuthScreen_Login(
                        isLoading = isLoading,
                        error = error,
                        canUseSavedCredentials = canUseSavedCredentials,
                        onSavedCredentialRequest = onSavedCredentialRequest,
                        onLoginRequest = { email, password ->
                            onLoginRequest(email.toString(), password.toString())
                        },
                        onRegisterRequest = {
                            onClearErrors()
                            scope.launch {
                                state.animateScrollToPage(1)
                            }
                        },
                        onForgotPassword = {
                            onForgotPassword(it.toString()) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(getString(Res.string.login_forgot_password_success_title))
                                }
                            }
                        },
                    )

                    1 -> AuthScreen_Register(
                        isLoading = isLoading,
                        error = error,
                        step = registrationStep,
                        canRegisterWithPasskey = canRegisterWithPasskey,
                        onLoginRequest = {
                            onClearErrors()
                            scope.launch {
                                state.animateScrollToPage(0)
                            }
                        },
                        onCodeRequest = { onRegistrationCodeRequest(it.toString()) },
                        onCodeEntered = { onRegistrationCodeEntered(it.toString()) },
                        onBack = onRegistrationBack,
                        onRegisterWithPasskeyRequest = onRegisterWithPasskeyRequest,
                        onRegisterRequest = { onRegisterRequest(it.toString()) },
                    )
                }

                Spacer(Modifier.weight(1f))

                Image(
                    painter = painterResource(Res.drawable.people),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                    contentScale = ContentScale.Inside,
                    alignment = Alignment.BottomCenter,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).padding(bottom = 8.dp).padding(horizontal = 36.dp)
                )
            }
        }
    }
}

@Composable
private fun AuthScreen_Form(
    isLoading: Boolean,
    error: Throwable?,
    isValid: Boolean,
    title: String,
    switchText: String,
    onSwitch: () -> Unit,
    submitText: String,
    onSubmit: () -> Unit,
    auxText: String? = null,
    onAux: () -> Unit = {},
    isPasskeysSupported: Boolean = false,
    showSubmit: Boolean = true,
    content: @Composable () -> Unit
) {
    Image(
        painter = painterResource(resource = Res.drawable.banner),
        contentDescription = null,
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .padding(16.dp)
    )

    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(bottom = 32.dp)
    )

    content()

    AnimatedVisibility(error != null) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Icon(imageVector = MaterialSymbols.Error, contentDescription = null, modifier = Modifier.padding(8.dp))

                val serverException = error as? ServerException
                val message = if (serverException != null) {
                    when (serverException.errorCode) {
                        Error.ERROR_USER_NOT_REGISTERED -> stringResource(Res.string.login_error_user_not_registered)
                        Error.ERROR_INCORRECT_PASSWORD_OR_EMAIL -> stringResource(Res.string.login_error_invalid_credentials)
                        Error.ERROR_INVALID_VERIFICATION_CODE -> stringResource(Res.string.register_error_invalid_code)
                        Error.ERROR_EMAIL_NOT_FOUND -> stringResource(Res.string.register_error_email_not_found)
                        Error.ERROR_MEMBER_IS_NOT_ACTIVE -> stringResource(Res.string.register_error_member_not_active)
                        Error.ERROR_USER_ALREADY_REGISTERED -> stringResource(Res.string.register_error_already_registered)
                        Error.ERROR_PASSWORD_NOT_SAFE_ENOUGH -> stringResource(Res.string.register_error_password_not_safe)
                        Error.ERROR_PASSWORD_NOT_SET -> stringResource(
                            if (isPasskeysSupported) Res.string.login_error_password_not_set else Res.string.login_error_password_not_set_desktop
                        )
                        else -> stringResource(Res.string.login_error_unknown, serverException.message ?: unknown())
                    }
                } else if (error is PasskeyException) {
                    stringResource(Res.string.login_error_passkey, error.message ?: unknown())
                } else {
                    error.toString()
                }
                Text(text = message, modifier = Modifier.padding(vertical = 8.dp).padding(end = 8.dp), style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        if (auxText != null) {
            TextButton(
                enabled = !isLoading,
                onClick = onAux,
                modifier = Modifier.weight(1f).padding(end = 4.dp)
            ) { Text(auxText) }
        }
        OutlinedButton(
            enabled = !isLoading,
            onClick = onSwitch,
            modifier = Modifier.weight(1f).padding(end = 4.dp)
        ) { Text(switchText) }
        if (showSubmit) {
            Button(
                enabled = isValid && !isLoading,
                onClick = onSubmit,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            ) { Text(submitText) }
        }
    }
}

@Composable
private fun AuthScreen_Login(
    isLoading: Boolean = false,
    error: Throwable? = null,
    canUseSavedCredentials: Boolean = false,
    onSavedCredentialRequest: () -> Unit = {},
    onLoginRequest: (email: CharSequence, password: CharSequence) -> Unit,
    onRegisterRequest: () -> Unit,
    onForgotPassword: (email: CharSequence) -> Job,
) {
    val email = rememberTextFieldState()
    val password = rememberTextFieldState()

    val valid = email.text.isNotBlank() && password.text.isNotBlank()

    var showingForgotPasswordDialog by remember { mutableStateOf(false) }
    if (showingForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showingForgotPasswordDialog = false },
            title = { Text(stringResource(Res.string.login_forgot_password_dialog_title)) },
            text = {
                Column {
                    Text(stringResource(Res.string.login_forgot_password_dialog_message))
                    OutlinedTextField(
                        state = email,
                        enabled = !isLoading,
                        label = { Text(stringResource(Res.string.email)) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        lineLimits = TextFieldLineLimits.SingleLine,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showingForgotPasswordDialog = false
                    onForgotPassword(email.text).invokeOnCompletion {
                        showingForgotPasswordDialog = false
                    }
                }) {
                    Text(stringResource(Res.string.login_forgot_password_dialog_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showingForgotPasswordDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }

    AuthScreen_Form(
        isLoading = isLoading,
        error = error,
        isValid = valid,
        title = GenderedStringResource.LoginTitle.stringResource(),
        switchText = stringResource(Res.string.register_action),
        onSwitch = onRegisterRequest,
        submitText = stringResource(Res.string.login_action),
        onSubmit = {
            onLoginRequest(email.text, password.text)
        },
        auxText = stringResource(Res.string.login_forgot_password),
        onAux = { showingForgotPasswordDialog = true },
        isPasskeysSupported = canUseSavedCredentials,
    ) {
        if (canUseSavedCredentials) {
            Button(
                enabled = !isLoading,
                onClick = onSavedCredentialRequest,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            ) {
                Icon(MaterialSymbols.Security, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(Res.string.login_saved_credential_action))
            }
            Text(
                text = stringResource(Res.string.login_or_type),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
            )
        }
        OutlinedTextField(
            state = email,
            enabled = !isLoading,
            label = { Text(stringResource(Res.string.email)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(top = 4.dp)
                .semantics {
                    // The account's username is its email: password managers fill both kinds of fields.
                    contentType = ContentType.Username + ContentType.EmailAddress
                },
            lineLimits = TextFieldLineLimits.SingleLine,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        )
        PasswordFormField(
            state = password,
            label = stringResource(Res.string.password),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(top = 4.dp),
            enabled = !isLoading,
            showNextButton = true,
        )
    }
}

@Composable
private fun AuthScreen_Register(
    isLoading: Boolean = false,
    error: Throwable? = null,
    step: RegistrationStep,
    canRegisterWithPasskey: Boolean,
    onLoginRequest: () -> Unit,
    onCodeRequest: (email: CharSequence) -> Unit,
    onCodeEntered: (code: CharSequence) -> Unit,
    onBack: () -> Unit,
    onRegisterWithPasskeyRequest: () -> Unit,
    onRegisterRequest: (password: CharSequence) -> Unit,
) {
    when (step) {
        RegistrationStep.Email -> AuthScreen_Register_Email(isLoading, error, onLoginRequest, onCodeRequest)
        is RegistrationStep.Code -> AuthScreen_Register_Code(isLoading, error, step.email, onLoginRequest, onBack, onCodeRequest, onCodeEntered)
        is RegistrationStep.Method -> AuthScreen_Register_Method(
            isLoading, error, step.email, canRegisterWithPasskey, onLoginRequest, onBack, onRegisterWithPasskeyRequest, onRegisterRequest,
        )
    }
}

/** Asks for the email of the account, to send it the code that proves it's the user's. */
@Composable
private fun AuthScreen_Register_Email(
    isLoading: Boolean,
    error: Throwable?,
    onLoginRequest: () -> Unit,
    onCodeRequest: (email: CharSequence) -> Unit,
) {
    val email = rememberTextFieldState()

    AuthScreen_Form(
        isLoading = isLoading,
        error = error,
        isValid = email.text.isNotBlank(),
        title = stringResource(Res.string.register_title),
        switchText = stringResource(Res.string.login_action),
        onSwitch = onLoginRequest,
        submitText = stringResource(Res.string.register_continue),
        onSubmit = { onCodeRequest(email.text) },
    ) {
        Text(
            text = stringResource(Res.string.register_email_message),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        )
        OutlinedTextField(
            state = email,
            enabled = !isLoading,
            label = { Text(stringResource(Res.string.email)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(top = 8.dp)
                .semantics {
                    // The platform suggests the user's own email.
                    contentType = ContentType.EmailAddress
                },
            lineLimits = TextFieldLineLimits.SingleLine,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
            onKeyboardAction = { if (email.text.isNotBlank()) onCodeRequest(email.text) },
        )
    }
}

/** Asks for the code emailed to [email]. */
@Composable
private fun AuthScreen_Register_Code(
    isLoading: Boolean,
    error: Throwable?,
    email: String,
    onLoginRequest: () -> Unit,
    onBack: () -> Unit,
    onCodeRequest: (email: CharSequence) -> Unit,
    onCodeEntered: (code: CharSequence) -> Unit,
) {
    val code = rememberTextFieldState()
    val isValid = code.text.trim().length == 6

    AuthScreen_Form(
        isLoading = isLoading,
        error = error,
        isValid = isValid,
        title = stringResource(Res.string.register_title),
        switchText = stringResource(Res.string.login_action),
        onSwitch = onLoginRequest,
        submitText = stringResource(Res.string.register_continue),
        onSubmit = { onCodeEntered(code.text) },
        auxText = stringResource(Res.string.register_back),
        onAux = onBack,
    ) {
        Text(
            text = stringResource(Res.string.register_code_message, email),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        )
        OutlinedTextField(
            state = code,
            enabled = !isLoading,
            label = { Text(stringResource(Res.string.register_code)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(top = 8.dp)
                .semantics {
                    // iOS fills it from the email, Android from a notification.
                    contentType = ContentType.SmsOtpCode
                },
            lineLimits = TextFieldLineLimits.SingleLine,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            onKeyboardAction = { if (isValid) onCodeEntered(code.text) },
        )
        TextButton(
            enabled = !isLoading,
            onClick = { onCodeRequest(email) },
            modifier = Modifier.padding(horizontal = 8.dp),
        ) { Text(stringResource(Res.string.register_code_resend)) }
    }
}

/**
 * Asks how the account will sign in: a passkey (recommended, where the platform supports them), or a password.
 */
@Composable
private fun AuthScreen_Register_Method(
    isLoading: Boolean,
    error: Throwable?,
    email: String,
    canRegisterWithPasskey: Boolean,
    onLoginRequest: () -> Unit,
    onBack: () -> Unit,
    onRegisterWithPasskeyRequest: () -> Unit,
    onRegisterRequest: (password: CharSequence) -> Unit,
) {
    var usePassword by remember { mutableStateOf(!canRegisterWithPasskey) }

    // Where the password manager takes the account's username from, when saving the new password.
    val username = rememberTextFieldState(email)
    val password = rememberTextFieldState()
    val passwordConfirm = rememberTextFieldState()

    val valid = password.text.isNotBlank() && password.text == passwordConfirm.text

    AuthScreen_Form(
        isLoading = isLoading,
        error = error,
        isValid = valid,
        title = stringResource(Res.string.register_title),
        switchText = stringResource(Res.string.login_action),
        onSwitch = onLoginRequest,
        submitText = stringResource(Res.string.register_action),
        onSubmit = { onRegisterRequest(password.text) },
        auxText = stringResource(Res.string.register_back),
        onAux = onBack,
        isPasskeysSupported = canRegisterWithPasskey,
        showSubmit = usePassword,
    ) {
        if (!usePassword) {
            PasskeyExplanationCard(
                isLoading = isLoading,
                onCreatePasskey = onRegisterWithPasskeyRequest,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            )
            TextButton(
                enabled = !isLoading,
                onClick = { usePassword = true },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            ) { Text(stringResource(Res.string.register_password_instead)) }
        } else {
            Text(
                text = stringResource(Res.string.register_password_message),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            )
            OutlinedTextField(
                state = username,
                readOnly = true,
                label = { Text(stringResource(Res.string.email)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(top = 4.dp)
                    .semantics {
                        contentType = ContentType.Username + ContentType.EmailAddress
                    },
                lineLimits = TextFieldLineLimits.SingleLine,
            )
            PasswordFormField(
                state = password,
                label = stringResource(Res.string.password),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(top = 4.dp),
                enabled = !isLoading,
                semanticsIsNewPassword = true,
                showNextButton = true,
            )
            PasswordFormField(
                state = passwordConfirm,
                label = stringResource(Res.string.confirm_password),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(top = 4.dp),
                enabled = !isLoading,
                semanticsIsNewPassword = true,
                showNextButton = false,
            )
            if (canRegisterWithPasskey) {
                TextButton(
                    enabled = !isLoading,
                    onClick = { usePassword = false },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                ) { Text(stringResource(Res.string.register_passkey_instead)) }
            }
        }
    }
}
