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
import androidx.navigation.toRoute
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import com.russhwolf.settings.ExperimentalSettingsApi
import io.github.vinceglb.filekit.coil.addPlatformFileSupport
import org.centrexcursionistalcoi.app.nav.Destination
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.ui.screen.HomeScreen
import org.centrexcursionistalcoi.app.ui.screen.LendingCreationScreen
import org.centrexcursionistalcoi.app.ui.screen.LendingSignUpScreen
import org.centrexcursionistalcoi.app.ui.screen.LendingsManagementScreen
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
                        popUpTo(navController.graph.id) {
                            inclusive = true
                        }
                    }
                },
                onNotLoggedIn = {
                    navController.navigate(Destination.Login) {
                        popUpTo(navController.graph.id) {
                            inclusive = true
                        }
                    }
                },
            )
        }
        composable<Destination.Login> {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Destination.Loading) {
                        popUpTo(navController.graph.id) {
                            inclusive = true
                        }
                    }
                },
            )
        }
        composable<Destination.Home> {
            HomeScreen(
                onManageLendingsRequested = {
                    navController.navigate(Destination.LendingsManagement)
                },
                onLendingSignUpRequested = {
                    navController.navigate(Destination.LendingSignUp)
                },
                onShoppingListConfirmed = {
                    navController.navigate(Destination.LendingCreation(it))
                },
            )
        }

        composable<Destination.LendingsManagement> {
            LendingsManagementScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable<Destination.LendingSignUp> {
            LendingSignUpScreen(
                onBackRequested = { navController.navigateUp() }
            )
        }
        composable<Destination.LendingCreation> { bse ->
            val route = bse.toRoute<Destination.LendingCreation>()
            val items = route.items

            LaunchedEffect(items) {
                // If there are no items, go back
                if (items.isEmpty()) navController.popBackStack()
            }

            LendingCreationScreen(
                shoppingList = items,
                onLendingCreated = {
                    navController.navigate(Destination.Home) {
                        popUpTo<Destination.Home>()
                    }
                }
            ) { navController.navigateUp() }
        }
    }
    LaunchedEffect(navController) {
        onNavHostReady(navController)
    }
}
