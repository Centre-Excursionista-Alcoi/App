package ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import ui.state.SharedApplicationState

abstract class BaseScreen(
    private val title: (@Composable () -> String)? = null,
    private val canGoBack: Boolean = false
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val title = title?.invoke()

        LaunchedEffect(Unit) {
            SharedApplicationState.title.tryEmit(title)
            SharedApplicationState.canGoBack.tryEmit(canGoBack)
        }
        LaunchedEffect(navigator) {
            SharedApplicationState.navigator.tryEmit(navigator)
        }
    }
}
