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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import com.diamondedge.logging.logging
import io.github.sudarshanmhasrup.localina.api.LocalinaApp
import io.github.vinceglb.filekit.coil.addPlatformFileSupport
import io.ktor.http.Url
import kotlinx.coroutines.CancellationException
import org.centrexcursionistalcoi.app.nav.DeepLinks
import org.centrexcursionistalcoi.app.nav.Destination
import org.centrexcursionistalcoi.app.nav.LocalTransitionContext
import org.centrexcursionistalcoi.app.nav.canOpenLinks
import org.centrexcursionistalcoi.app.nav.rememberNavigator
import org.centrexcursionistalcoi.app.platform.PlatformAppUpdates
import org.centrexcursionistalcoi.app.push.PushNotification
import org.centrexcursionistalcoi.app.ui.dialog.ErrorDialog
import org.centrexcursionistalcoi.app.ui.screen.spaces.SpaceDetailsScreen
import org.centrexcursionistalcoi.app.ui.screen.spaces.SpaceLendingCreationScreen
import org.centrexcursionistalcoi.app.ui.screen.spaces.SpaceLendingDetailsScreen
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
import org.centrexcursionistalcoi.app.ui.screen.SecurityScreen
import org.centrexcursionistalcoi.app.ui.screen.SettingsScreen
import org.centrexcursionistalcoi.app.ui.screen.admin.LendingManagementScreen
import org.centrexcursionistalcoi.app.ui.theme.AppTheme
import org.centrexcursionistalcoi.app.viewmodel.PlatformInitializerViewModel
import org.centrexcursionistalcoi.app.viewmodel.SessionExpiryViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val log = logging()

@Composable
fun MainApp(
    url: Url? = null,
    pushNotification: PushNotification? = null,
    model: PlatformInitializerViewModel = koinViewModel { parametersOf(url, pushNotification) },
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
            val targetDestination by model.targetDestination.collectAsState()

            if (isReady) {
                App(targetDestination)
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
@OptIn(ExperimentalSharedTransitionApi::class)
private fun App(
    afterLoad: Destination? = null,
) {
    val navigator = rememberNavigator(Destination.Loading)

    // Links that reach the app by a callback instead of starting it (iOS, see DeepLinks). While the app is loading
    // or the user is logging in there's nowhere to open one yet, so it waits here until the user is in.
    var waitingLink by remember { mutableStateOf<Destination?>(null) }
    val incomingLink by DeepLinks.pending.collectAsState()
    LaunchedEffect(incomingLink) {
        val url = incomingLink ?: return@LaunchedEffect
        val destination = try {
            DeepLinks.fromUrl(url)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.e(e) { "Could not resolve the link: $url" }
            null
        }
        DeepLinks.consume(url)
        if (destination == null) {
            log.w { "Ignoring a link that doesn't point anywhere in the app: $url" }
        } else if (navigator.current.canOpenLinks()) {
            log.i { "Opening link: $destination" }
            if (navigator.current != destination) navigator.navigate(Destination.backStackFor(destination))
        } else {
            log.i { "Not ready to open the link yet, will open it once the user is in: $destination" }
            waitingLink = destination
        }
    }

    val errorState by GlobalAsyncErrorHandler.error.collectAsState()
    errorState?.let { error ->
        ErrorDialog(exception = error) { GlobalAsyncErrorHandler.clearError() }
    }

    // A RemoteRepository call anywhere in the app can find the session has expired/been invalidated
    // server-side; treat it the same everywhere instead of leaving whichever screen triggered it to fail
    // unhandled (see #620). SessionExpiryViewModel does the actual silent-relogin-or-logout work (Android only
    // for now, see CredentialsStore) -- kept out of this Composable since it's critical, side-effecting logic.
    // It's handed the navigation callback directly instead of exposing an event Flow for this Composable to
    // collect, so there's no LaunchedEffect needed here just to forward it.
    koinViewModel<SessionExpiryViewModel> {
        parametersOf({ navigator.navigateClearingStack(Destination.Login()) })
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
                            // A link the user opened wins over what a notification would open
                            val opening = waitingLink ?: afterLoad
                            waitingLink = null
                            log.i { "User is logged in. Navigating to: $opening" }
                            if (opening is Destination.Main) {
                                // Main is the root of the stack: replace it, instead of putting another over it
                                navigator.navigateClearingStack(opening)
                            } else {
                                navigator.navigateClearingStack(Destination.Main())
                                opening?.let { navigator.navigate(Destination.backStackFor(it)) }
                            }
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
                        showingFemecvAccount = route.showingFemecvAccount,
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
                        onEditMemoryRequest = {
                            navigator.navigate(Destination.MemoryEditor(it.id))
                        },
                        onSpaceDetailsRequested = { navigator.navigate(Destination.SpaceDetails(it)) },
                        onSpaceBookRequested = { navigator.navigate(Destination.SpaceLendingCreation(it)) },
                        onSpaceLendingClick = { navigator.navigate(Destination.SpaceLendingDetails(it)) },
                    )
                }
                contentEntries(navigator)
            },
        )
    }
}
