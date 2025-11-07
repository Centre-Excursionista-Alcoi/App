package org.centrexcursionistalcoi.app

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import com.russhwolf.settings.ExperimentalSettingsApi
import io.github.sudarshanmhasrup.localina.api.LocalinaApp
import io.github.vinceglb.filekit.coil.addPlatformFileSupport
import kotlin.reflect.typeOf
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.nav.Destination
import org.centrexcursionistalcoi.app.nav.LocalTransitionContext
import org.centrexcursionistalcoi.app.nav.UuidNavType
import org.centrexcursionistalcoi.app.ui.dialog.ErrorDialog
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.ui.screen.ActivityMemoryEditor
import org.centrexcursionistalcoi.app.ui.screen.HomeScreen
import org.centrexcursionistalcoi.app.ui.screen.InventoryItemsScreen
import org.centrexcursionistalcoi.app.ui.screen.LendingCreationScreen
import org.centrexcursionistalcoi.app.ui.screen.LendingPickupScreen
import org.centrexcursionistalcoi.app.ui.screen.LendingReturnScreen
import org.centrexcursionistalcoi.app.ui.screen.LendingSignUpScreen
import org.centrexcursionistalcoi.app.ui.screen.LendingsManagementScreen
import org.centrexcursionistalcoi.app.ui.screen.LoadingScreen
import org.centrexcursionistalcoi.app.ui.screen.LoginScreen
import org.centrexcursionistalcoi.app.ui.screen.LogoutScreen
import org.centrexcursionistalcoi.app.ui.screen.SettingsScreen
import org.centrexcursionistalcoi.app.ui.theme.AppTheme
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

    AppTheme {
        LocalinaApp {
            val isReady by model.isReady.collectAsState()

            if (isReady) {
                App(onNavHostReady)
            } else {
                LoadingBox()
            }
        }
    }
}

@Composable
@OptIn(ExperimentalSettingsApi::class)
fun App(
    onNavHostReady: suspend (NavController) -> Unit = {}
) {
    val navController = rememberNavController()

    val errorState by GlobalAsyncErrorHandler.error.collectAsState()
    errorState?.let { error ->
        ErrorDialog(exception = error) { GlobalAsyncErrorHandler.clearError() }
    }

    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = Destination.Loading,
            modifier = Modifier.fillMaxSize().imePadding(),
        ) {
            destination<Destination.Loading> {
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
            destination<Destination.Login> {
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
            destination<Destination.Logout> {
                LogoutScreen(
                    afterLogout = {
                        navController.navigate(Destination.Loading) {
                            popUpTo(navController.graph.id) {
                                inclusive = true
                            }
                        }
                    }
                )
            }
            destination<Destination.Home> {
                HomeScreen(
                    onClickInventoryItemType = { type ->
                        navController.navigate(Destination.InventoryItems(type))
                    },
                    onManageLendingsRequested = {
                        navController.navigate(Destination.LendingsManagement)
                    },
                    onLendingSignUpRequested = {
                        navController.navigate(Destination.LendingSignUp)
                    },
                    onShoppingListConfirmed = {
                        navController.navigate(Destination.LendingCreation(it))
                    },
                    onMemoryEditorRequested = {
                        navController.navigate(Destination.LendingMemoryEditor(it))
                    },
                    onLogoutRequested = {
                        navController.navigate(Destination.Logout) {
                            popUpTo(navController.graph.id) {
                                inclusive = true
                            }
                        }
                    },
                )
            }
            destination<Destination.Settings> {
                SettingsScreen {
                    navController.navigateUp()
                }
            }

            destination<Destination.InventoryItems> { route ->
                val typeId = route.typeId
                val displayName = route.displayName

                InventoryItemsScreen(
                    typeId = typeId,
                    typeDisplayName = displayName,
                    onBack = { navController.navigateUp() }
                )
            }

            destination<Destination.LendingsManagement> {
                LendingsManagementScreen(
                    onLendingPickupRequest = {
                        navController.navigate(Destination.LendingPickup(it.id))
                    },
                    onLendingReturnRequest = {
                        navController.navigate(Destination.LendingReturn(it.id))
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            destination<Destination.LendingSignUp> {
                LendingSignUpScreen(
                    onSignUpComplete = {
                        navController.navigate(Destination.Home) {
                            popUpTo<Destination.Home>()
                        }
                    },
                    onBackRequested = { navController.navigateUp() }
                )
            }
            destination<Destination.LendingCreation> { route ->
                val items = route.shoppingList

                LaunchedEffect(items) {
                    // If there are no items, go back
                    if (items.isEmpty()) navController.popBackStack()
                }

                LendingCreationScreen(
                    originalShoppingList = items,
                    onLendingCreated = {
                        navController.navigate(Destination.Home) {
                            popUpTo<Destination.Home>()
                        }
                    }
                ) { navController.navigateUp() }
            }
            destination<Destination.LendingPickup> { route ->
                val lendingId = route.lendingId

                LendingPickupScreen(
                    lendingId = lendingId,
                    onBack = { navController.navigateUp() },
                    onComplete = { navController.popBackStack() },
                )
            }
            destination<Destination.LendingReturn> { route ->
                val lendingId = route.lendingId

                LendingReturnScreen(
                    lendingId = lendingId,
                    onBack = { navController.navigateUp() },
                    onComplete = { navController.popBackStack() },
                )
            }
            destination<Destination.LendingMemoryEditor> { route ->
                val lendingId = route.lendingId

                ActivityMemoryEditor(lendingId) { navController.navigateUp() }
            }
        }
    }
    LaunchedEffect(navController) {
        onNavHostReady(navController)
    }
}

context(scope: SharedTransitionScope)
inline fun <reified D: Destination> NavGraphBuilder.destination(
    noinline content: @Composable (D) -> Unit
) {
    composable<D>(
        typeMap = mapOf(
            typeOf<Uuid>() to UuidNavType,
        ),
    ) { bse ->
        val route = bse.toRoute<D>()

        CompositionLocalProvider(LocalTransitionContext provides (scope to this@composable)) {
            content(route)
        }
    }
}
