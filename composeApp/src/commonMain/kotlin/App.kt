import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import ui.screen.LoadingScreen
import ui.theme.AppTheme

@Composable
fun App() {
    AppTheme {
        Navigator(LoadingScreen)
    }
}
