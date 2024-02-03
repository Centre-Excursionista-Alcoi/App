package ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.russhwolf.settings.ExperimentalSettingsApi
import storage.SettingsKeys
import storage.settings
import ui.screen.LoadingScreen

/**
 * Observes the current value of [SettingsKeys.CONFIRMATION_ID].
 * If it's set to a non-null value, the navigator is redirected to [LoadingScreen].
 */
@Composable
@OptIn(ExperimentalSettingsApi::class)
@Suppress("UnusedReceiverParameter")
fun Screen.ConfirmationStatusWatcher(
    onUpdateReceived: (id: String?) -> Unit = {}
) {
    val navigator = LocalNavigator.currentOrThrow

    DisposableEffect(Unit) {
        val listener = settings.addStringOrNullListener(SettingsKeys.CONFIRMATION_ID) { id ->
            if (id != null) {
                navigator.push(LoadingScreen())
            }
            onUpdateReceived(id)
        }
        onDispose { listener.deactivate() }
    }
}
