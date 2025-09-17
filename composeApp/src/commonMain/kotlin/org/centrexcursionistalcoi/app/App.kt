package org.centrexcursionistalcoi.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.russhwolf.settings.ExperimentalSettingsApi
import org.centrexcursionistalcoi.app.nav.Destination
import org.centrexcursionistalcoi.app.screen.HomeScreen
import org.centrexcursionistalcoi.app.screen.LoadingScreen
import org.centrexcursionistalcoi.app.screen.LoginScreen
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory

@Composable
fun MainApp(
    authFlowFactory: CodeAuthFlowFactory,
    onNavHostReady: suspend (NavController) -> Unit = {}
) {
    MaterialTheme {
        App(authFlowFactory, onNavHostReady)
    }
}

@Composable
@OptIn(ExperimentalSettingsApi::class, ExperimentalOpenIdConnect::class)
fun App(
    authFlowFactory: CodeAuthFlowFactory,
    onNavHostReady: suspend (NavController) -> Unit = {}
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Destination.Loading
    ) {
        composable<Destination.Loading> {
            LoadingScreen(
                onLoggedIn = { navController.navigate(Destination.Home) },
                onNotLoggedIn = { navController.navigate(Destination.Login) },
            )
        }
        composable<Destination.Login> {
            LoginScreen(
                authFlowFactory,
                onLoginSuccess = {
                    navController.navigate(Destination.Loading) {
                        popUpTo(Destination.Login)
                    }
                },
            )
        }
        composable<Destination.Home> {
            HomeScreen()
        }
    }
    LaunchedEffect(navController) {
        onNavHostReady(navController)
    }
}
