package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.centrexcursionistalcoi.app.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    model: LoginViewModel = viewModel { LoginViewModel() },
    onLoginSuccess: () -> Unit,
) {
    val isLoggingIn by model.isLoggingIn.collectAsState()

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
                        Button(
                            onClick = { model.login().invokeOnCompletion { onLoginSuccess() } }
                        ) { Text("Login") }
                    }
                }
            }
        }
    }
}
