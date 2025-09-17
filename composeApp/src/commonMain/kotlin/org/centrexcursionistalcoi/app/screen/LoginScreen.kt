package org.centrexcursionistalcoi.app.screen

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.centrexcursionistalcoi.app.viewmodel.LoginViewModel
import org.publicvalue.multiplatform.oidc.OpenIdConnectException
import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory

@Composable
fun LoginScreen(
    authFlowFactory: CodeAuthFlowFactory,
    model: LoginViewModel = viewModel { LoginViewModel(authFlowFactory) },
    onLoginSuccess: () -> Unit,
) {
    val discoveryComplete by model.discoveryComplete.collectAsState()
    val isLoading by model.isLoading.collectAsState()
    val isLoggingIn by model.isLoggingIn.collectAsState()
    val isStoringToken by model.isStoringToken.collectAsState()
    val error by model.error.collectAsState()

    LaunchedEffect(Unit) { model.load() }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
                contentAlignment = Alignment.Center
            ) {
                /*LaunchedEffect(discoveryComplete) {
                    if (discoveryComplete) {
                        model.login().invokeOnCompletion {
                            onLoginSuccess()
                        }
                    }
                }*/

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!discoveryComplete) {
                        CircularProgressIndicator()

                        Text(
                            text = "Running discovery...",
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else if (isLoading) {
                        CircularProgressIndicator()

                        Text(
                            text = if (isLoggingIn) "Logging in..." else if (isStoringToken) "Storing token..." else "Loading...",
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        error?.let { err ->
                            when (err) {
                                is OpenIdConnectException.AuthenticationCancelled -> {
                                    Text(
                                        text = "Login cancelled",
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }

                                is OpenIdConnectException.TechnicalFailure -> {
                                    Text(
                                        text = "Technical failure! Please check your network connection and try again.",
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }

                                else -> {
                                    Text(
                                        text = "Error: ${err.message}",
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }

                            Button(
                                onClick = { model.login().invokeOnCompletion { onLoginSuccess() } }
                            ) { Text("Try again") }
                        } ?: run {
                            Button(
                                onClick = { model.login().invokeOnCompletion { onLoginSuccess() } }
                            ) { Text("Login") }
                        }
                    }
                }
            }
        }
    }
}
