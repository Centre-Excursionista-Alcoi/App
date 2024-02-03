package ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import backend.supabase
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.russhwolf.settings.ExperimentalSettingsApi
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import storage.SettingsKeys
import storage.settings
import ui.reusable.LoadingIndicator

class LoadingScreen : Screen {
    private val auth = supabase.auth

    @Composable
    @OptIn(ExperimentalSettingsApi::class)
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        var status: SessionStatus? by remember { mutableStateOf(null) }
        var confirmationId: String? by remember { mutableStateOf(null) }

        LaunchedEffect(navigator) {
            auth.sessionStatus.collect { status = it }
        }
        LaunchedEffect(settings) {
            confirmationId = settings.getStringOrNull(SettingsKeys.CONFIRMATION_ID) ?: ""
        }
        LaunchedEffect(status, confirmationId) {
            when {
                !confirmationId.isNullOrBlank() -> navigator.push(WaitingConfirmationScreen())
                status is SessionStatus.Authenticated -> navigator.push(MainScreen())
                status == SessionStatus.NotAuthenticated -> navigator.push(AuthScreen())
                status == SessionStatus.NetworkError -> TODO("Network error!")
                status == SessionStatus.LoadingFromStorage -> {}
            }
        }

        LoadingIndicator()
    }
}
