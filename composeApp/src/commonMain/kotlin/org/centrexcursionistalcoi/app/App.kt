package org.centrexcursionistalcoi.app

import androidx.compose.animation.ExperimentalSharedTransitionApi
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
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import com.diamondedge.logging.logging
import com.russhwolf.settings.ExperimentalSettingsApi
import io.github.sudarshanmhasrup.localina.api.LocaleUpdater
import io.github.sudarshanmhasrup.localina.api.LocalinaApp
import io.github.vinceglb.filekit.coil.addPlatformFileSupport
import io.ktor.http.Url
import org.centrexcursionistalcoi.app.nav.Destination
import org.centrexcursionistalcoi.app.nav.LocalTransitionContext
import org.centrexcursionistalcoi.app.nav.NullableUuidNavType
import org.centrexcursionistalcoi.app.nav.UuidNavType
import org.centrexcursionistalcoi.app.platform.PlatformAppUpdates
import org.centrexcursionistalcoi.app.push.LocalNotifications.checkIsSelf
import org.centrexcursionistalcoi.app.push.PushNotification
import org.centrexcursionistalcoi.app.storage.SETTINGS_LANGUAGE
import org.centrexcursionistalcoi.app.storage.settings
import org.centrexcursionistalcoi.app.ui.dialog.ErrorDialog
import org.centrexcursionistalcoi.app.ui.dialog.UpdateAvailableDialog
import org.centrexcursionistalcoi.app.ui.dialog.UpdateProgressDialog
import org.centrexcursionistalcoi.app.ui.dialog.UpdateRestartRequiredDialog
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.ui.screen.ActivityMemoryEditor
import org.centrexcursionistalcoi.app.ui.screen.AuthScreen
import org.centrexcursionistalcoi.app.ui.screen.InventoryItemTypeDetailsScreen
import org.centrexcursionistalcoi.app.ui.screen.LendingCreationScreen
import org.centrexcursionistalcoi.app.ui.screen.LendingDetailsScreen
import org.centrexcursionistalcoi.app.ui.screen.LendingSignUpScreen
import org.centrexcursionistalcoi.app.ui.screen.LoadingScreen
import org.centrexcursionistalcoi.app.ui.screen.LogoutScreen
import org.centrexcursionistalcoi.app.ui.screen.MainScreen
import org.centrexcursionistalcoi.app.ui.screen.SettingsScreen
import org.centrexcursionistalcoi.app.ui.screen.admin.LendingManagementScreen
import org.centrexcursionistalcoi.app.ui.theme.AppTheme
import org.centrexcursionistalcoi.app.viewmodel.ManagementViewModel
import org.centrexcursionistalcoi.app.viewmodel.PlatformInitializerViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.reflect.KClass
import kotlin.reflect.typeOf
import kotlin.uuid.Uuid

private val log = logging()

@Composable
fun MainApp(
    url: Url? = null,
    pushNotification: PushNotification? = null,
    model: PlatformInitializerViewModel = koinViewModel { parametersOf(url) },
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
                    log.i { "Setting locale to: $lang" }
                    LocaleUpdater.updateLocale(lang)
                }
            }

            if (isReady) {
                LaunchedEffect(Unit) {
                    log.d { "Platform is ready..." }
                }

                fun <N: PushNotification.LendingUpdated> destination(
                    notification: N,
                    forAdmin: (N) -> Destination? = { null },
                    forUser: (N) -> Destination? = { null }
                ): Destination? {
                    return if (notification.checkIsSelf()) forUser(notification)
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
                LaunchedEffect(Unit) {
                    log.d { "Platform not ready..." }
                }

                LoadingBox()
            }
        }
    }
}

@Composable
@OptIn(ExperimentalSettingsApi::class, ExperimentalSharedTransitionApi::class)
fun App(
    afterLoad: Destination? = null,
    onNavHostReady: suspend (NavController) -> Unit = {}
) {
    val navController = rememberNavController()

    val errorState by GlobalAsyncErrorHandler.error.collectAsState()
    errorState?.let { error ->
        ErrorDialog(exception = error) { GlobalAsyncErrorHandler.clearError() }
    }

    val updateAvailable by PlatformAppUpdates.updateAvailable.collectAsState(initial = false)
    val updateProgress by PlatformAppUpdates.updateProgress.collectAsState(initial = null)
    val restartRequired by PlatformAppUpdates.restartRequired.collectAsState(initial = false)
    if (updateAvailable && updateProgress == null) UpdateAvailableDialog()
    if (updateProgress != null && !restartRequired) UpdateProgressDialog()
    if (restartRequired) UpdateRestartRequiredDialog()

    SharedTransitionLayout {
        LaunchedEffect(Unit) { log.d { "Rendering NavHost..." } }

        NavHost(
            navController = navController,
            startDestination = Destination.Loading,
            modifier = Modifier.fillMaxSize().imePadding(),
        ) {
            destination<Destination.Loading> {
                LoadingScreen(
                    onLoggedIn = {
                        log.i { "User is logged in. Navigating to: $afterLoad" }
                        navController.navigate(afterLoad ?: Destination.Main()) {
                            popUpTo(navController.graph.id) {
                                inclusive = true
                            }
                        }
                    },
                    onNotLoggedIn = {
                        log.i { "User is not logged in. Navigating to login screen..." }
                        navController.navigate(Destination.Login()) {
                            popUpTo(navController.graph.id) {
                                inclusive = true
                            }
                        }
                    },
                )
            }
            destination<Destination.Login> { route ->
                val changedPassword = route.changedPassword

                AuthScreen(
                    changedPassword = changedPassword,
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
            destination<Destination.Main> { route ->
                val showingAdminItemTypeId = route.showingAdminItemTypeId
                val showingAdminLendingsScreen = route.showingAdminLendingsScreen
                val managementViewModel: ManagementViewModel = koinInject()

                MainScreen(
                    showingAdminItemTypeId = showingAdminItemTypeId,
                    showingAdminLendingsScreen = showingAdminLendingsScreen,
                    onLendingSignUpRequested = {
                        navController.navigate(Destination.LendingSignUp)
                    },
                    onLendingClick = {
                        navController.navigate(Destination.LendingDetails(it))
                    },
                    onMemoryEditorRequested = {
                        navController.navigate(Destination.MemoryEditor(it.id))
                    },
                    onCreateMemoryRequested = {
                        navController.navigate(Destination.MemoryEditor(null))
                    },
                    onOtherUserLendingClick = {
                        navController.navigate(Destination.Admin.LendingManagement(it))
                    },
                    onDeleteLendingRequest = managementViewModel::delete,
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
                SettingsScreen(
                    onBack = {
                        navController.navigateUp()
                    },
                    onDeleteAccount = {
                        navController.navigate(Destination.Loading) {
                            popUpTo(navController.graph.id) {
                                inclusive = true
                            }
                        }
                    },
                )
            }

            destination<Destination.LendingDetails> { route ->
                val lendingId = route.lendingId

                LendingDetailsScreen(
                    lendingId = lendingId,
                    onMemoryEditorRequested = {
                        navController.navigate(Destination.MemoryEditor(lendingId))
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
                        navController.navigate(Destination.Main()) {
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
                        navController.navigate(Destination.Main()) {
                            popUpTo<Destination.Main>()
                        }
                    }
                ) { navController.navigateUp() }
            }
            destination<Destination.MemoryEditor> { route ->
                val lendingId = route.lendingId

                ActivityMemoryEditor(lendingId) { navController.navigateUp() }
            }
        }
    }
    LaunchedEffect(navController) {
        onNavHostReady(navController)
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
context(scope: SharedTransitionScope)
fun <D: Destination> NavGraphBuilder.destination(
    kClass: KClass<D>,
    content: @Composable (D) -> Unit
) {
    composable(
        kClass,
        typeMap = mapOf(
            typeOf<Uuid>() to UuidNavType,
            typeOf<Uuid?>() to NullableUuidNavType,
        ),
    ) { bse ->
        val route = bse.toRoute<D>(kClass)

        LaunchedEffect(Unit) {
            log.d { "Rendering screen ${kClass.simpleName}" }
        }

        CompositionLocalProvider(LocalTransitionContext provides (scope to this@composable)) {
            content(route)
        }
    }
}


@OptIn(ExperimentalSharedTransitionApi::class)
context(scope: SharedTransitionScope)
inline fun <reified D: Destination> NavGraphBuilder.destination(
    noinline content: @Composable (D) -> Unit
) {
    destination(D::class, content)
}
