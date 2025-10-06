package org.centrexcursionistalcoi.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import com.russhwolf.settings.ExperimentalSettingsApi
import io.github.vinceglb.filekit.coil.addPlatformFileSupport
import org.centrexcursionistalcoi.app.nav.Destination
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.ui.screen.HomeScreen
import org.centrexcursionistalcoi.app.ui.screen.LoadingScreen
import org.centrexcursionistalcoi.app.ui.screen.LoginScreen
import org.centrexcursionistalcoi.app.viewmodel.PlatformInitializerViewModel

@Composable
fun MainApp(
    model: PlatformInitializerViewModel = viewModel { PlatformInitializerViewModel() },
    onNavHostReady: suspend (NavController) -> Unit = {}
) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                addPlatformFileSupport()
            }
            .build()
    }

    MaterialTheme {
        val isReady by model.isReady.collectAsState()

        if (isReady) {
            App(onNavHostReady)
        } else {
            LoadingBox()
        }
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
        startDestination = Destination.Loading
    ) {
        composable<Destination.Loading> {
            LoadingScreen(
                onLoggedIn = {
                    navController.navigate(Destination.Home) {
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
                    navController.navigate(Destination.Loading) {
                        popUpTo<Destination.Login>()
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
