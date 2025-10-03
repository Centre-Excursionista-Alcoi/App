package org.centrexcursionistalcoi.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.russhwolf.settings.ExperimentalSettingsApi
import org.centrexcursionistalcoi.app.nav.Destination
import org.centrexcursionistalcoi.app.platform.PlatformLoadLogic
import org.centrexcursionistalcoi.app.ui.screen.HomeScreen
import org.centrexcursionistalcoi.app.ui.screen.LoadingScreen
import org.centrexcursionistalcoi.app.ui.screen.LoginScreen

@Composable
fun MainApp(
    onNavHostReady: suspend (NavController) -> Unit = {}
) {
    MaterialTheme {
        App(onNavHostReady)
    }
}

@Composable
@OptIn(ExperimentalSettingsApi::class)
fun App(
    onNavHostReady: suspend (NavController) -> Unit = {}
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Destination.Loading()
    ) {
        composable<Destination.Loading> { entry ->
            val route = entry.toRoute<Destination.Loading>()
            LoadingScreen(
                onLoggedIn = {
                    navController.navigate(route.redirectTo ?: Destination.Home) {
                        popUpTo<Destination.Loading>()
                    }
                },
                onNotLoggedIn = {
                    navController.navigate(Destination.Login) {
                        popUpTo<Destination.Loading>()
                    }
                },
            )
        }
        composable<Destination.Login> {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Destination.Loading()) {
                        popUpTo<Destination.Login>()
                    }
                },
            )
        }
        composable<Destination.Home> {
            if (!PlatformLoadLogic.isReady()) {
                SideEffect {
                    navController.navigate(Destination.Loading(Destination.Home)) {
                        popUpTo<Destination.Home>()
                    }
                }
            } else {
                HomeScreen()
            }
        }
    }
    LaunchedEffect(navController) {
        onNavHostReady(navController)
    }
}
