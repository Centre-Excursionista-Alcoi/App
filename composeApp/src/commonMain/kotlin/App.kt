import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import ui.screen.LoadingScreen

@Composable
fun App() {
    // AppTheme {
    MaterialTheme {
        Navigator(LoadingScreen)
    }
    // }
}
