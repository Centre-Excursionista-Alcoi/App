@file:OptIn(ExperimentalSharedTransitionApi::class)

package org.centrexcursionistalcoi.app.bridge

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.lending_details_cancel
import cea_app.composeapp.generated.resources.lending_details_cancel_confirm_message
import cea_app.composeapp.generated.resources.lending_details_cancel_confirm_title
import cea_app.composeapp.generated.resources.memory_create
import cea_app.composeapp.generated.resources.shopping_list_selected
import cea_app.composeapp.generated.resources.shopping_list_view
import io.github.sudarshanmhasrup.localina.api.LocalinaApp
import org.centrexcursionistalcoi.app.contentEntries
import org.centrexcursionistalcoi.app.destination
import org.centrexcursionistalcoi.app.nav.Destination
import org.centrexcursionistalcoi.app.nav.LocalTransitionContext
import org.centrexcursionistalcoi.app.nav.MainTab
import org.centrexcursionistalcoi.app.nav.Navigator
import org.centrexcursionistalcoi.app.nav.destinationSavedStateConfiguration
import org.centrexcursionistalcoi.app.typing.ShoppingList
import org.centrexcursionistalcoi.app.ui.dialog.DeleteDialog
import org.centrexcursionistalcoi.app.ui.dialog.LendingsHistoryDialog
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Add
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Receipt
import org.centrexcursionistalcoi.app.ui.page.main.ActivitiesPage
import org.centrexcursionistalcoi.app.ui.page.main.HomePage
import org.centrexcursionistalcoi.app.ui.page.main.LendingPage
import org.centrexcursionistalcoi.app.ui.page.main.LendingsPage
import org.centrexcursionistalcoi.app.ui.page.main.ManagementPage
import org.centrexcursionistalcoi.app.ui.page.main.ProfilePage
import org.centrexcursionistalcoi.app.ui.page.main.SpacesTab
import org.centrexcursionistalcoi.app.viewmodel.spaces.SpacesViewModel
import org.centrexcursionistalcoi.app.ui.screen.AuthScreen
import org.centrexcursionistalcoi.app.ui.screen.LendingsActionBarIcons
import org.centrexcursionistalcoi.app.ui.screen.LogoutScreen
import org.centrexcursionistalcoi.app.ui.theme.AppTheme
import org.centrexcursionistalcoi.app.viewmodel.MainScreenViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import platform.UIKit.UIViewController
import kotlin.uuid.Uuid

/*
 * Compose "islands": the existing Compose UI, in a view controller the native SwiftUI UI embeds for every screen it
 * doesn't replace yet. They go away as the native screens land.
 *
 * Whatever an island opens next is up to Swift: a tab's island reports it through `onNavigate`, so it is pushed on
 * the native navigation stack, while a destination's island keeps its own back stack (what the screen opens from
 * there is rendered by the island itself) and says through `onClose` when the last screen goes back.
 */

/**
 * A tab of the main screen, without its bars: the native shell draws the tab bar and the toolbar.
 *
 * @param onNavigate Opens a screen over the tabs.
 * @param onTabRequested Switches to another tab.
 */
fun composeTabViewController(
    tab: MainTab,
    scrollToFemecvAccount: Boolean,
    onNavigate: (Destination) -> Unit,
    onTabRequested: (MainTab) -> Unit,
): UIViewController = ComposeUIViewController {
    AppTheme {
        LocalinaApp {
            TransitionHost {
                TabIsland(tab, scrollToFemecvAccount, onNavigate, onTabRequested)
            }
        }
    }
}

/** The spaces of the lending page (and the user's bookings of them), until the native UI has its own. */
fun composeSpacesViewController(
    hasItemsSelected: Boolean,
    onNavigate: (Destination) -> Unit,
): UIViewController = ComposeUIViewController {
    AppTheme {
        LocalinaApp {
            TransitionHost {
                val model: SpacesViewModel = koinViewModel()
                val spaces by model.spaces.collectAsState()
                val lendings by model.myLendings.collectAsState()
                SpacesTab(
                    spaces = spaces.orEmpty(),
                    lendings = lendings.orEmpty(),
                    hasItemsSelected = hasItemsSelected,
                    onDetailsRequested = { onNavigate(Destination.SpaceDetails(it)) },
                    onBookRequested = { onNavigate(Destination.SpaceLendingCreation(it)) },
                    onLendingClick = { onNavigate(Destination.SpaceLendingDetails(it)) },
                )
            }
        }
    }
}

/**
 * A screen that opens over the tabs (see [contentEntries]), or the login or logout screens.
 *
 * @param onSessionChange Called when the screen sends the user somewhere the island doesn't handle: back to
 * [Destination.Loading] after logging in or out.
 * @param onClose Called when the user goes back from the first screen.
 */
fun composeDestinationViewController(
    destination: Destination,
    onSessionChange: (Destination) -> Unit,
    onClose: () -> Unit,
): UIViewController = ComposeUIViewController {
    AppTheme {
        LocalinaApp {
            val backStack = rememberNavBackStack(destinationSavedStateConfiguration, destination)
            val navigator = remember(backStack) { IslandNavigator(backStack, onSessionChange, onClose) }

            SharedTransitionLayout {
                NavDisplay(
                    backStack = navigator.backStack,
                    modifier = Modifier.fillMaxSize().imePadding(),
                    onBack = { navigator.goBack() },
                    sharedTransitionScope = this,
                    entryProvider = entryProvider {
                        destination<Destination.Login> { route ->
                            AuthScreen(
                                changedPassword = route.changedPassword,
                                onLoginSuccess = { navigator.navigateClearingStack(Destination.Loading) },
                            )
                        }
                        destination<Destination.Logout> {
                            LogoutScreen(afterLogout = { navigator.navigateClearingStack(Destination.Loading) })
                        }
                        contentEntries(navigator)
                    },
                )
            }
        }
    }
}

private class IslandNavigator(
    backStack: NavBackStack<NavKey>,
    private val onSessionChange: (Destination) -> Unit,
    private val onClose: () -> Unit,
) : Navigator(backStack) {
    override fun goBack(): Boolean {
        if (backStack.size <= 1) {
            onClose()
            return false
        }
        return super.goBack()
    }

    // Replacing the whole stack means the session changed (logged in, out, or the account was deleted)
    override fun navigateClearingStack(destination: Destination) = onSessionChange(destination)

    // "Back to the main screen" is closing the island: the main screen is what's under it
    override fun navigatePoppingUpTo(destination: Destination, popUpTo: kotlin.reflect.KClass<out Destination>) =
        onClose()
}

/** Gives the screens the shared transition scopes the navigation of the app would, which some of them need. */
@Composable
private fun TransitionHost(content: @Composable () -> Unit) {
    SharedTransitionLayout {
        val sharedScope = this
        AnimatedContent(targetState = Unit) {
            CompositionLocalProvider(LocalTransitionContext provides (sharedScope to this)) { content() }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TabIsland(
    tab: MainTab,
    scrollToFemecvAccount: Boolean,
    onNavigate: (Destination) -> Unit,
    onTabRequested: (MainTab) -> Unit,
    model: MainScreenViewModel = koinViewModel(),
) {
    val profile by model.profile.collectAsState()
    val lendings by model.lendings.collectAsState()
    val activeUserLending by model.activeUserLending.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var shoppingList by remember { mutableStateOf<ShoppingList>(emptyMap()) }
    var showingLendingHistory by remember { mutableStateOf(false) }
    var cancellingLending by remember { mutableStateOf(false) }

    if (showingLendingHistory) {
        LendingsHistoryDialog(
            lendings = lendings.orEmpty().filterNot { it.status().isPending() },
            onClick = {
                showingLendingHistory = false
                onNavigate(Destination.LendingDetails(it))
            },
            onDismissRequest = { showingLendingHistory = false },
        )
    }
    if (cancellingLending) {
        activeUserLending?.let { lending ->
            DeleteDialog(
                title = stringResource(Res.string.lending_details_cancel_confirm_title),
                message = stringResource(Res.string.lending_details_cancel_confirm_message),
                buttonText = stringResource(Res.string.lending_details_cancel),
                onDelete = {
                    model.cancelLending(lending).also {
                        it.invokeOnCompletion {
                            cancellingLending = false
                            onTabRequested(MainTab.HOME)
                        }
                    }
                },
                onDismissRequested = { cancellingLending = false },
            )
        } ?: run { cancellingLending = false }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (tab == MainTab.LENDINGS && shoppingList.isNotEmpty()) {
                ExtendedFloatingActionButton(onClick = { onNavigate(Destination.LendingCreation(shoppingList)) }) {
                    Icon(MaterialSymbols.Receipt, stringResource(Res.string.shopping_list_view))
                    Spacer(Modifier.padding(4.dp))
                    Text(stringResource(Res.string.shopping_list_selected, shoppingList.toList().sumOf { it.second }))
                }
            }
            if (tab == MainTab.ACTIVITIES) {
                FloatingActionButton(onClick = { onNavigate(Destination.LendingMemoryEditor(null)) }) {
                    Icon(MaterialSymbols.Add, stringResource(Res.string.memory_create))
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (tab == MainTab.LENDINGS || tab == MainTab.LENDING) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    LendingsActionBarIcons(
                        activeLending = activeUserLending,
                        lendings = lendings?.filter { it.user.sub == profile?.sub },
                        onCancelLendingRequest = { cancellingLending = true },
                        onLendingHistoryRequest = { showingLendingHistory = true },
                    )
                }
            }
            when (tab) {
                MainTab.HOME -> HomePage(onFemecvAccountRequested = { onTabRequested(MainTab.PROFILE) })
                MainTab.LENDINGS -> LendingsPage(
                    onAddInsuranceRequested = { onTabRequested(MainTab.PROFILE) },
                    onItemTypeDetailsRequested = { onNavigate(Destination.ItemTypeDetails(it)) },
                    onLendingSignUpRequested = { onNavigate(Destination.LendingSignUp) },
                    onLendingHistoryRequest = { showingLendingHistory = true },
                    onShoppingListChanged = { shoppingList = it },
                    onSpaceDetailsRequested = { onNavigate(Destination.SpaceDetails(it)) },
                    onSpaceBookRequested = { onNavigate(Destination.SpaceLendingCreation(it)) },
                    onSpaceLendingClick = { onNavigate(Destination.SpaceLendingDetails(it)) },
                )
                MainTab.LENDING -> LendingPage(
                    onCancelLendingRequest = { cancellingLending = true },
                    onLendingHistoryRequest = { showingLendingHistory = true },
                    onMemoryEditorRequested = { onNavigate(Destination.LendingMemoryEditor(it.id)) },
                    snackbarHostState = snackbarHostState,
                )
                MainTab.ACTIVITIES -> ActivitiesPage { onNavigate(Destination.MemoryEditor(it.id)) }
                MainTab.MANAGEMENT -> ManagementPage(
                    snackbarHostState = snackbarHostState,
                    selectedItem = null,
                    onGiveRequested = { onNavigate(Destination.Admin.LendingManagement(it)) },
                    onReceiveRequested = { onNavigate(Destination.Admin.LendingManagement(it)) },
                    onSpaceLendingClick = { onNavigate(Destination.SpaceLendingDetails(it)) },
                )
                MainTab.PROFILE -> ProfilePage(
                    scrollToFemecvAccount = scrollToFemecvAccount,
                    onFemecvAccountShown = {},
                )
            }
        }
    }
}
