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
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
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
import org.centrexcursionistalcoi.app.nav.rememberNavigator
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
import org.centrexcursionistalcoi.app.viewmodel.PlatformInitializerViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val log = logging()

@Composable
fun MainApp(
    url: Url? = null,
    pushNotification: PushNotification? = null,
    model: PlatformInitializerViewModel = koinViewModel { parametersOf(url) },
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
                App(afterLoad ?: startDestination)
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
private fun App(
    afterLoad: Destination? = null,
) {
    val navigator = rememberNavigator(Destination.Loading)

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
        LaunchedEffect(Unit) { log.d { "Rendering NavDisplay..." } }

        NavDisplay(
            backStack = navigator.backStack,
            modifier = Modifier.fillMaxSize().imePadding(),
            onBack = { navigator.goBack() },
            sharedTransitionScope = this,
            entryProvider = entryProvider {
                destination<Destination.Loading> {
                    LoadingScreen(
                        onLoggedIn = {
                            log.i { "User is logged in. Navigating to: $afterLoad" }
                            navigator.navigateClearingStack(Destination.Main())
                            afterLoad?.let { navigator.navigate(Destination.backStackFor(it)) }
                        },
                        onNotLoggedIn = {
                            log.i { "User is not logged in. Navigating to login screen..." }
                            navigator.navigateClearingStack(Destination.Login())
                        },
                    )
                }
                destination<Destination.Login> { route ->
                    val changedPassword = route.changedPassword

                    AuthScreen(
                        changedPassword = changedPassword,
                        onLoginSuccess = {
                            navigator.navigateClearingStack(Destination.Loading)
                        },
                    )
                }
                destination<Destination.Logout> {
                    LogoutScreen(
                        afterLogout = {
                            navigator.navigateClearingStack(Destination.Loading)
                        }
                    )
                }
                destination<Destination.Main> { route ->
                    val showingAdminItemTypeId = route.showingAdminItemTypeId
                    val showingAdminLendingsScreen = route.showingAdminLendingsScreen

                    MainScreen(
                        showingAdminItemTypeId = showingAdminItemTypeId,
                        showingAdminLendingsScreen = showingAdminLendingsScreen,
                        onLendingSignUpRequested = {
                            navigator.navigate(Destination.LendingSignUp)
                        },
                        onLendingClick = {
                            navigator.navigate(Destination.LendingDetails(it))
                        },
                        onMemoryEditorRequested = {
                            navigator.navigate(Destination.LendingMemoryEditor(it.id))
                        },
                        onCreateMemoryRequested = {
                            navigator.navigate(Destination.LendingMemoryEditor(null))
                        },
                        onOtherUserLendingClick = {
                            navigator.navigate(Destination.Admin.LendingManagement(it))
                        },
                        onShoppingListConfirmed = {
                            navigator.navigate(Destination.LendingCreation(it))
                        },
                        onSettingsRequested = {
                            navigator.navigate(Destination.Settings)
                        },
                        onItemTypeDetailsRequested = { type ->
                            navigator.navigate(Destination.ItemTypeDetails(type))
                        },
                        onLogoutRequested = {
                            navigator.navigateClearingStack(Destination.Logout)
                        },
                    )
                }
                destination<Destination.Settings> {
                    SettingsScreen(
                        onBack = {
                            navigator.goBack()
                        },
                        onDeleteAccount = {
                            navigator.navigateClearingStack(Destination.Loading)
                        },
                    )
                }

                destination<Destination.LendingDetails> { route ->
                    val lendingId = route.lendingId

                    LendingDetailsScreen(
                        lendingId = lendingId,
                        onMemoryEditorRequested = {
                            navigator.navigate(Destination.LendingMemoryEditor(lendingId))
                        },
                        onBack = { navigator.goBack() }
                    )
                }

                destination<Destination.ItemTypeDetails> { route ->
                    val typeId = route.typeId
                    val displayName = route.displayName

                    InventoryItemTypeDetailsScreen(
                        typeId = typeId,
                        typeDisplayName = displayName,
                        onBack = { navigator.goBack() },
                    )
                }

                destination<Destination.Admin.LendingManagement> { route ->
                    val lendingId = route.lendingId

                    LendingManagementScreen(
                        lendingId = lendingId,
                        onBack = { navigator.goBack() },
                    )
                }

                destination<Destination.LendingSignUp> {
                    LendingSignUpScreen(
                        onSignUpComplete = {
                            navigator.navigatePoppingUpTo(Destination.Main(), Destination.Main::class)
                        },
                        onBackRequested = { navigator.goBack() }
                    )
                }
                destination<Destination.LendingCreation> { route ->
                    val items = route.shoppingList

                    LaunchedEffect(items) {
                        // If there are no items, go back
                        if (items.isEmpty()) navigator.goBack()
                    }

                    LendingCreationScreen(
                        originalShoppingList = items,
                        onLendingCreated = {
                            navigator.navigatePoppingUpTo(Destination.Main(), Destination.Main::class)
                        }
                    ) { navigator.goBack() }
                }
                destination<Destination.LendingMemoryEditor> { route ->
                    val lendingId = route.lendingId

                    ActivityMemoryEditor(lendingId) { navigator.goBack() }
                }
            },
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
context(scope: SharedTransitionScope)
private inline fun <reified D : Destination> EntryProviderScope<NavKey>.destination(
    noinline content: @Composable (D) -> Unit
) {
    entry<D> { route ->
        LaunchedEffect(Unit) {
            log.d { "Rendering screen ${D::class.simpleName}" }
        }

        val animatedContentScope = LocalNavAnimatedContentScope.current
        CompositionLocalProvider(LocalTransitionContext provides (scope to animatedContentScope)) {
            content(route)
        }
    }
}
