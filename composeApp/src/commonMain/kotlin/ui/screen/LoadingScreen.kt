package ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import backend.supabase
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import ui.reusable.LoadingIndicator

object LoadingScreen : Screen {
    private val auth = supabase.auth

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        LaunchedEffect(navigator) {
            auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> TODO("Not yet implemented")
                    SessionStatus.NotAuthenticated -> navigator.push(AuthScreen())
                    SessionStatus.NetworkError -> TODO("Network error!")
                    SessionStatus.LoadingFromStorage -> {}
                }
            }
        }

        LoadingIndicator()
    }
}
