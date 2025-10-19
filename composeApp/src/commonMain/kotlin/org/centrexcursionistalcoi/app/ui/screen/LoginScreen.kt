package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.viewmodel.LoginViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun LoginScreen(
    model: LoginViewModel = viewModel { LoginViewModel() },
    onLoginSuccess: () -> Unit,
) {
    val isLoggingIn by model.isLoggingIn.collectAsState()

    LoginScreen(
        isLoggingIn = isLoggingIn,
        onLoginRequest = { model.login().invokeOnCompletion { onLoginSuccess() } }
    )
}

@Composable
private fun LoginScreen(
    isLoggingIn: Boolean,
    onLoginRequest: () -> Unit,
) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
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

                        Button(
                            onClick = onLoginRequest,
                            modifier = Modifier.padding(top = 16.dp)
                        ) { Text(stringResource(Res.string.login_action)) }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreen_Preview() {
    LoginScreen(false, {})
}
