package org.centrexcursionistalcoi.app.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import org.centrexcursionistalcoi.app.viewmodel.LoginViewModel
import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory

@Composable
fun LoginScreen(
    authFlowFactory: CodeAuthFlowFactory,
    model: LoginViewModel = viewModel { LoginViewModel(authFlowFactory) },
    onLoginSuccess: () -> Unit,
) {
    val isLoading by model.isLoading.collectAsState()

    LaunchedEffect(Unit) { model.load() }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            Button(
                onClick = {
                    model.login().invokeOnCompletion {
                        onLoginSuccess()
                    }
                },
                enabled = !isLoading,
            ) {
                Text("Login")
            }
        }
    }
}
