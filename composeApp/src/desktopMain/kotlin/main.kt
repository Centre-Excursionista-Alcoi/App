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
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.flow.MutableStateFlow
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import ui.theme.AppTheme
import ui.window.WindowTopBar

val windowTitle = MutableStateFlow("App")

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
fun main() = application {
    val windowState = rememberWindowState()
    val title by windowTitle.collectAsState("")

    Napier.base(DebugAntilog())

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = title,
        undecorated = true,
        resizable = windowState.placement == WindowPlacement.Floating
    ) {
        AppTheme {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                WindowTopBar(windowState, title, ::exitApplication)

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
