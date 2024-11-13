package org.centrexcursionistalcoi.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import org.centrexcursionistalcoi.app.composition.LocalNavController
import org.centrexcursionistalcoi.app.platform.ui.PlatformTheme
import org.centrexcursionistalcoi.app.route.Loading
import org.centrexcursionistalcoi.app.screen.HomeScreen
import org.centrexcursionistalcoi.app.screen.LoadingScreen
import org.centrexcursionistalcoi.app.screen.LoginScreen
import org.centrexcursionistalcoi.app.screen.RegisterScreen
import org.centrexcursionistalcoi.app.screen.ReservationScreen
import org.centrexcursionistalcoi.app.screen.composable
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun AppRoot() {
    val navController = rememberNavController()

    PlatformTheme {
        CompositionLocalProvider(
            LocalNavController provides navController
        ) {
            NavHost(navController, startDestination = Loading) {
                composable(LoadingScreen)
                composable(LoginScreen)
                composable(RegisterScreen)
                composable(HomeScreen)
                composable(ReservationScreen)
            }
        }
    }
}
