package ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import backend.supabase
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import ui.screen.MainScreen

/**
 * Observes the current session status, and if the user is logged in, the navigator is redirected to
 * the [MainScreen].
 */
@Composable
@Suppress("UnusedReceiverParameter")
fun Screen.SessionStatusWatcher() {
    val navigator = LocalNavigator.currentOrThrow

    LaunchedEffect(Unit) {
        supabase.auth.sessionStatus.collect {
            when (it) {
                is SessionStatus.Authenticated -> navigator.push(MainScreen())
                SessionStatus.NetworkError -> TODO("Handle network error")
                SessionStatus.LoadingFromStorage -> {
                    /* Do nothing */
                }

                SessionStatus.NotAuthenticated -> {
                    /* Do nothing */
                }
            }
        }
    }
}
