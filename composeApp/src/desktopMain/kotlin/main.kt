import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import ui.state.SharedApplicationState
import ui.theme.AppTheme
import ui.window.WindowTopBar

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
fun main() = application {
    val windowState = rememberWindowState()

    val canGoBack by SharedApplicationState.canGoBack.collectAsState(null)
    val navigator by SharedApplicationState.navigator.collectAsState(null)
    val title by SharedApplicationState.title.collectAsState(null)

    Napier.base(DebugAntilog())

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = title ?: "CEA App",
        undecorated = true,
        resizable = windowState.placement == WindowPlacement.Floating
    ) {
        AppTheme {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.type != PointerEventType.Release && event.button == PointerButton.Back) {
                                    if (canGoBack == true && navigator?.canPop == true) {
                                        navigator?.pop()
                                    }
                                }
                            }
                        }
                    }
            ) {
                WindowTopBar(windowState, title ?: "CEA App", ::exitApplication)

                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    App()
                }
            }
        }
    }
}

@Preview
@Composable
fun AppDesktopPreview() {
    App()
}
