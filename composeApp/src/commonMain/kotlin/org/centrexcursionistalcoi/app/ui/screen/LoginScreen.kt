package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.ui.reusable.form.PasswordFormField
import org.centrexcursionistalcoi.app.viewmodel.LoginViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

private val emailRegex = "^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$".toRegex()

@Composable
fun LoginScreen(
    model: LoginViewModel = viewModel { LoginViewModel() },
    onLoginSuccess: () -> Unit,
) {
    val isLoggingIn by model.isLoggingIn.collectAsState()
    val isRegistering by model.isRegistering.collectAsState()
    val registerError by model.registerError.collectAsState()

    LoginScreen(
        isLoggingIn = isLoggingIn,
        onLoginRequest = { model.login().invokeOnCompletion { onLoginSuccess() } },
        isRegistering = isRegistering,
        registerError = registerError,
        onRegisterRequest = model::register,
        onClearErrors =  model::clearErrors,
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
private fun LoginScreen(
    isLoggingIn: Boolean,
    onLoginRequest: () -> Unit,
    isRegistering: Boolean,
    registerError: String?,
    onRegisterRequest: (username: String, name: String, email: String, password: String) -> Deferred<Boolean>,
    onClearErrors: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val state = rememberPagerState { 2 }

    Scaffold { paddingValues ->
        HorizontalPager(
            state = state,
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            userScrollEnabled = false,
        ) { page ->
            when (page) {
                0 -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (isLoggingIn) {
                                    CircularProgressIndicator()

                                    Text(
                                        text = "Logging in...",
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                } else {
                                    LoginScreen_Login(onLoginRequest) {
                                        scope.launch { state.animateScrollToPage(1) }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LoginScreen_Register(isRegistering, registerError, onClearErrors) { username, name, email, password ->
                            val job = onRegisterRequest(username, name, email, password)
                            job.invokeOnCompletion {
                                val success = job.getCompleted()
                                if (success) {
                                    // Go back to login page
                                    scope.launch {
                                        state.animateScrollToPage(0)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreen_Login_Preview() {
    Column {
        LoginScreen_Login({}) {}
    }
}

@Composable
fun LoginScreen_Login(onLoginRequest: () -> Unit, onRegisterRequest: () -> Unit) {
    Image(
        painter = painterResource(resource = Res.drawable.banner),
        contentDescription = null,
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .padding(16.dp)
    )

    Text(
        text = stringResource(Res.string.login_title),
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
    )
    Text(
        text = stringResource(Res.string.login_message),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 8.dp),
        textAlign = TextAlign.Center,
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
    ) {
        OutlinedButton(
            onClick = onRegisterRequest,
            modifier = Modifier.weight(1f).padding(end = 4.dp)
        ) { Text(stringResource(Res.string.register_action)) }
        Button(
            onClick = onLoginRequest,
            modifier = Modifier.weight(1f).padding(start = 4.dp)
        ) { Text(stringResource(Res.string.login_action)) }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreen_Register_Preview() {
    Column {
        LoginScreen_Register { _, _, _, _ -> }
    }
}

@Composable
fun LoginScreen_Register(
    isLoading: Boolean = false,
    error: String? = null,
    onClearErrors: () -> Unit = {},
    onRegisterRequest: (username: String, name: String, email: String, password: String) -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    val password = rememberTextFieldState()
    val passwordConfirm = rememberTextFieldState()

    val emailFormatValid = email.isEmpty() || email.matches(emailRegex)
    val valid = username.isNotBlank() &&
            name.isNotBlank() &&
            email.isNotBlank() &&
            password.text.isNotBlank() &&
            password.text == passwordConfirm.text &&
            email.matches(emailRegex)

    Image(
        painter = painterResource(resource = Res.drawable.banner),
        contentDescription = null,
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .padding(16.dp)
    )

    Text(
        text = stringResource(Res.string.register_title),
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
    )

    OutlinedTextField(
        value = username,
        onValueChange = { username = it; onClearErrors() },
        enabled = !isLoading,
        label = { Text(stringResource(Res.string.register_username)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(top = 4.dp)
            .semantics {
                contentType = ContentType.Username
            },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Next),
    )
    OutlinedTextField(
        value = name,
        onValueChange = { name = it; onClearErrors() },
        enabled = !isLoading,
        label = { Text(stringResource(Res.string.register_name)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(top = 4.dp)
            .semantics {
                contentType = ContentType.PersonFullName
            },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
    )
    OutlinedTextField(
        value = email,
        onValueChange = { email = it; onClearErrors() },
        enabled = !isLoading,
        label = { Text(stringResource(Res.string.register_email)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(top = 4.dp)
            .semantics {
                contentType = ContentType.EmailAddress
            },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        isError = !emailFormatValid,
        supportingText = if (!emailFormatValid) {
            { Text(stringResource(Res.string.register_error_email_format)) }
        } else null,
        singleLine = true,
    )
    PasswordFormField(
        state = password,
        label = stringResource(Res.string.register_password),
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
        label = stringResource(Res.string.register_confirm_password),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(top = 4.dp),
        enabled = !isLoading,
        semanticsIsNewPassword = true,
        showNextButton = false,
    )

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
                Text(text = error ?: "", modifier = Modifier.padding(vertical = 8.dp).padding(end = 8.dp), style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    Button(
        enabled = valid && !isLoading,
        onClick = {
            onRegisterRequest(username, name, email, password.text.toString())
        },
        modifier = Modifier.padding(top = 16.dp)
    ) { Text(stringResource(Res.string.register_action)) }
}
