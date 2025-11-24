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
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
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
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.ui.reusable.ColumnWidthWrapper
import org.centrexcursionistalcoi.app.ui.reusable.form.PasswordFormField
import org.centrexcursionistalcoi.app.viewmodel.LoginViewModel
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoginScreen(
    changedPassword: Boolean = false,
    model: LoginViewModel = viewModel { LoginViewModel() },
    onLoginSuccess: () -> Unit,
) {
    val isLoading by model.isLoading.collectAsState()
    val error by model.error.collectAsState()

    LoginScreen(
        isLoading = isLoading,
        error = error,
        changedPassword = changedPassword,
        onLoginRequest = { nif, password -> model.login(nif, password, onLoginSuccess) },
        onRegisterRequest = { nif, password -> model.register(nif, password, onLoginSuccess) },
        onForgotPassword = { nif, ar -> model.forgotPassword(nif, ar) },
        onClearErrors = model::clearError,
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
private fun LoginScreen(
    isLoading: Boolean,
    error: Throwable?,
    changedPassword: Boolean,
    onLoginRequest: (nif: String, password: String) -> Unit,
    onRegisterRequest: (nif: String, password: String) -> Unit,
    onForgotPassword: (nif: String, afterRequest: () -> Unit) -> Job,
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
                    0 -> LoginScreen_Login(
                        isLoading = isLoading,
                        error = error,
                        onLoginRequest = { nif, password ->
                            onLoginRequest(nif.toString(), password.toString())
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

                    1 -> LoginScreen_Register(
                        isLoading = isLoading,
                        error = error,
                        onLoginRequest = {
                            onClearErrors()
                            scope.launch {
                                state.animateScrollToPage(0)
                            }
                        },
                        onRegisterRequest = { nif, password ->
                            onRegisterRequest(nif.toString(), password.toString())
                        },
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
fun LoginScreen_Form(
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
                Icon(imageVector = Icons.Default.Error, contentDescription = null, modifier = Modifier.padding(8.dp))

                val serverException = error as? ServerException
                val message = if (serverException != null) {
                    // TODO: Localize errors
                    when (serverException.errorCode) {
                        Error.ERROR_PASSWORD_NOT_SET -> "Password not set. Please register."
                        Error.ERROR_INCORRECT_PASSWORD_OR_NIF -> "Incorrect NIF or password."
                        else -> "Server error: ${serverException.message ?: "Unknown error"}"
                    }
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
        Button(
            enabled = isValid && !isLoading,
            onClick = onSubmit,
            modifier = Modifier.weight(1f).padding(start = 4.dp)
        ) { Text(submitText) }
    }
}

@Composable
fun LoginScreen_Login(
    isLoading: Boolean = false,
    error: Throwable? = null,
    onLoginRequest: (nif: CharSequence, password: CharSequence) -> Unit,
    onRegisterRequest: () -> Unit,
    onForgotPassword: (nif: CharSequence) -> Job,
) {
    val nif = rememberTextFieldState()
    val password = rememberTextFieldState()

    val valid = nif.text.isNotBlank() && password.text.isNotBlank()

    var showingForgotPasswordDialog by remember { mutableStateOf(false) }
    if (showingForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showingForgotPasswordDialog = false },
            title = { Text(stringResource(Res.string.login_forgot_password_dialog_title)) },
            text = {
                Column {
                    Text(stringResource(Res.string.login_forgot_password_dialog_message))
                    OutlinedTextField(
                        state = nif,
                        enabled = !isLoading,
                        label = { Text(stringResource(Res.string.nif)) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showingForgotPasswordDialog = false
                    onForgotPassword(nif.text).invokeOnCompletion {
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

    LoginScreen_Form(
        isLoading = isLoading,
        error = error,
        isValid = valid,
        title = stringResource(Res.string.login_title),
        switchText = stringResource(Res.string.register_action),
        onSwitch = onRegisterRequest,
        submitText = stringResource(Res.string.login_action),
        onSubmit = {
            onLoginRequest(nif.text, password.text)
        },
        auxText = stringResource(Res.string.login_forgot_password),
        onAux = { showingForgotPasswordDialog = true }
    ) {
        OutlinedTextField(
            state = nif,
            enabled = !isLoading,
            label = { Text(stringResource(Res.string.nif)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(top = 4.dp)
                .semantics {
                    contentType = ContentType.Username
                },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
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
    }
}

@Composable
fun LoginScreen_Register(
    isLoading: Boolean = false,
    error: Throwable? = null,
    onLoginRequest: () -> Unit,
    onRegisterRequest: (nif: CharSequence, password: CharSequence) -> Unit,
) {
    val nif = rememberTextFieldState()
    val password = rememberTextFieldState()
    val passwordConfirm = rememberTextFieldState()

    val valid = nif.text.isNotBlank() &&
            password.text.isNotBlank() &&
            password.text == passwordConfirm.text

    LoginScreen_Form(
        isLoading = isLoading,
        error = error,
        isValid = valid,
        title = stringResource(Res.string.register_title),
        switchText = stringResource(Res.string.login_action),
        onSwitch = onLoginRequest,
        submitText = stringResource(Res.string.register_action),
        onSubmit = {
            onRegisterRequest(nif.text, password.text)
        }
    ) {
        OutlinedTextField(
            state = nif,
            enabled = !isLoading,
            label = { Text(stringResource(Res.string.nif)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(top = 4.dp)
                .semantics {
                    contentType = ContentType.Username
                },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
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
    }
}
