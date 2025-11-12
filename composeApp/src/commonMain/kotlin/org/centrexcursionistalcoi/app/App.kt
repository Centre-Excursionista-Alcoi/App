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
import androidx.compose.runtime.remember
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
import io.github.aakira.napier.Napier
import io.github.sudarshanmhasrup.localina.api.LocaleUpdater
import io.github.sudarshanmhasrup.localina.api.LocalinaApp
import io.github.vinceglb.filekit.coil.addPlatformFileSupport
import io.ktor.http.Url
import kotlin.reflect.typeOf
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.nav.Destination
import org.centrexcursionistalcoi.app.nav.LocalTransitionContext
import org.centrexcursionistalcoi.app.nav.NullableUuidNavType
import org.centrexcursionistalcoi.app.nav.UuidNavType
import org.centrexcursionistalcoi.app.push.PushNotification
import org.centrexcursionistalcoi.app.storage.SETTINGS_LANGUAGE
import org.centrexcursionistalcoi.app.storage.settings
import org.centrexcursionistalcoi.app.ui.dialog.ErrorDialog
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.ui.screen.ActivityMemoryEditor
import org.centrexcursionistalcoi.app.ui.screen.InventoryItemTypeDetailsScreen
import org.centrexcursionistalcoi.app.ui.screen.LendingCreationScreen
import org.centrexcursionistalcoi.app.ui.screen.LendingDetailsScreen
import org.centrexcursionistalcoi.app.ui.screen.LendingSignUpScreen
import org.centrexcursionistalcoi.app.ui.screen.LoadingScreen
import org.centrexcursionistalcoi.app.ui.screen.LoginScreen
import org.centrexcursionistalcoi.app.ui.screen.LogoutScreen
import org.centrexcursionistalcoi.app.ui.screen.MainScreen
import org.centrexcursionistalcoi.app.ui.screen.SettingsScreen
import org.centrexcursionistalcoi.app.ui.screen.admin.InventoryItemsScreen
import org.centrexcursionistalcoi.app.ui.screen.admin.LendingManagementScreen
import org.centrexcursionistalcoi.app.ui.screen.admin.LendingsManagementScreen
import org.centrexcursionistalcoi.app.ui.theme.AppTheme
import org.centrexcursionistalcoi.app.viewmodel.PlatformInitializerViewModel

@Composable
fun MainApp(
    url: Url? = null,
    pushNotification: PushNotification? = null,
    model: PlatformInitializerViewModel = viewModel { PlatformInitializerViewModel(url) },
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
            val startDestination by model.startDestination.collectAsState()

            LaunchedEffect(Unit) {
                settings.getStringOrNull(SETTINGS_LANGUAGE)?.let { lang ->
                    Napier.i { "Setting locale to: $lang" }
                    LocaleUpdater.updateLocale(lang)
                }
            }

            if (isReady) {
                fun <N: PushNotification.LendingUpdated> destination(
                    notification: N,
                    forAdmin: (N) -> Destination? = { null },
                    forUser: (N) -> Destination? = { null }
                ): Destination? {
                    return if (notification.isSelf) forUser(notification)
                    else forAdmin(notification)
                }

                val afterLoad: Destination? = remember(pushNotification) {
                    when (pushNotification) {
                        // always admin notifications
                        is PushNotification.NewLendingRequest -> Destination.Admin.LendingManagement(pushNotification.lendingId)
                        is PushNotification.NewMemoryUpload -> Destination.Admin.LendingManagement(pushNotification.lendingId)
                        // always user notifications
                        is PushNotification.LendingCancelled -> null // the lending is cancelled, cannot show any info
                        is PushNotification.LendingConfirmed -> Destination.LendingDetails(
                            lendingId = pushNotification.lendingId
                        )
                        // could be either
                        is PushNotification.LendingTaken -> destination(
                            pushNotification,
                            forAdmin = { Destination.Admin.LendingManagement(pushNotification.lendingId) },
                            forUser = { Destination.LendingDetails(it.lendingId) },
                        )
                        is PushNotification.LendingPartiallyReturned -> destination(
                            pushNotification,
                            forAdmin = { Destination.Admin.LendingManagement(pushNotification.lendingId) },
                            forUser = { Destination.LendingDetails(it.lendingId) },
                        )
                        is PushNotification.LendingReturned -> destination(
                            pushNotification,
                            forAdmin = { Destination.Admin.LendingManagement(pushNotification.lendingId) },
                            forUser = { Destination.LendingDetails(it.lendingId) },
                        )
                        else -> null
                    }
                }
                App(afterLoad ?: startDestination, onNavHostReady)
            } else {
                LoadingBox()
            }
        }
    }
}

@Composable
@OptIn(ExperimentalSettingsApi::class)
fun App(
    afterLoad: Destination? = null,
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
                        Napier.i { "User is logged in. Navigating to: ${afterLoad ?: Destination.Main}" }
                        navController.navigate(afterLoad ?: Destination.Main) {
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
            destination<Destination.Main> {
                MainScreen(
                    onClickInventoryItemType = { type ->
                        navController.navigate(Destination.Admin.InventoryItems(type))
                    },
                    onManageLendingsRequested = {
                        navController.navigate(Destination.Admin.LendingsManagement)
                    },
                    onLendingSignUpRequested = {
                        navController.navigate(Destination.LendingSignUp)
                    },
                    onLendingClick = {
                        navController.navigate(Destination.LendingDetails(it))
                    },
                    onOtherUserLendingClick = {
                        navController.navigate(Destination.Admin.LendingManagement(it))
                    },
                    onShoppingListConfirmed = {
                        navController.navigate(Destination.LendingCreation(it))
                    },
                    onSettingsRequested = {
                        navController.navigate(Destination.Settings)
                    },
                    onItemTypeDetailsRequested = { type ->
                        navController.navigate(Destination.ItemTypeDetails(type))
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

            destination<Destination.LendingDetails> { route ->
                val lendingId = route.lendingId

                LendingDetailsScreen(
                    lendingId = lendingId,
                    onMemoryEditorRequested = {
                        navController.navigate(Destination.LendingMemoryEditor(lendingId))
                    },
                    onBack = { navController.navigateUp() }
                )
            }

            destination<Destination.ItemTypeDetails> { route ->
                val typeId = route.typeId
                val displayName = route.displayName

                InventoryItemTypeDetailsScreen(
                    typeId = typeId,
                    typeDisplayName = displayName,
                    onBack = { navController.navigateUp() },
                )
            }

            destination<Destination.Admin.InventoryItems> { route ->
                val typeId = route.typeId
                val displayName = route.displayName

                InventoryItemsScreen(
                    typeId = typeId,
                    typeDisplayName = displayName,
                    onBack = { navController.navigateUp() }
                )
            }

            destination<Destination.Admin.LendingsManagement> {
                LendingsManagementScreen(
                    onClickLending = { lendingId ->
                        navController.navigate(Destination.Admin.LendingManagement(lendingId))
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            destination<Destination.Admin.LendingManagement> { route ->
                val lendingId = route.lendingId

                LendingManagementScreen(
                    lendingId = lendingId,
                    onBack = { navController.navigateUp() },
                )
            }

            destination<Destination.LendingSignUp> {
                LendingSignUpScreen(
                    onSignUpComplete = {
                        navController.navigate(Destination.Main) {
                            popUpTo<Destination.Main>()
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
                        navController.navigate(Destination.Main) {
                            popUpTo<Destination.Main>()
                        }
                    }
                ) { navController.navigateUp() }
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
            typeOf<Uuid?>() to NullableUuidNavType,
        ),
    ) { bse ->
        val route = bse.toRoute<D>()

        CompositionLocalProvider(LocalTransitionContext provides (scope to this@composable)) {
            content(route)
        }
    }
}
