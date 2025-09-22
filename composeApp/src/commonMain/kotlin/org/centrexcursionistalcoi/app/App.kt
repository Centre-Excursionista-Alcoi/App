package org.centrexcursionistalcoi.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.russhwolf.settings.ExperimentalSettingsApi
import org.centrexcursionistalcoi.app.auth.UserData
import org.centrexcursionistalcoi.app.nav.Destination
import org.centrexcursionistalcoi.app.screen.HomeScreen
import org.centrexcursionistalcoi.app.screen.LoadingScreen
import org.centrexcursionistalcoi.app.screen.LoginScreen

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
    var userData by remember { mutableStateOf<UserData?>(null) }

    NavHost(
        navController = navController,
        startDestination = Destination.Loading
    ) {
        composable<Destination.Loading> {
            LoadingScreen(
                onLoggedIn = { name, groups ->
                    userData = UserData(name, groups)

                    navController.navigate(Destination.Home) {
                        popUpTo(Destination.Loading)
                    }
                },
                onNotLoggedIn = {
                    navController.navigate(Destination.Login) {
                        popUpTo(Destination.Loading)
                    }
                },
            )
        }
        composable<Destination.Login> {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Destination.Loading) {
                        popUpTo(Destination.Login)
                    }
                },
            )
        }
        composable<Destination.Home> {
            userData?.let {
                HomeScreen(it)
            } ?: run {
                LaunchedEffect(Unit) {
                    navController.navigate(Destination.Loading) {
                        popUpTo(Destination.Home)
                    }
                }
            }
        }
    }
    LaunchedEffect(navController) {
        onNavHostReady(navController)
    }
}
