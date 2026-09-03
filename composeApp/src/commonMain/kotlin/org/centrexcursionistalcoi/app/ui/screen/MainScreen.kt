package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.admin
import cea_app.composeapp.generated.resources.app_name
import cea_app.composeapp.generated.resources.error_access_denied
import cea_app.composeapp.generated.resources.force_sync
import cea_app.composeapp.generated.resources.icon
import cea_app.composeapp.generated.resources.lending_details_cancel
import cea_app.composeapp.generated.resources.lending_details_cancel_confirm_message
import cea_app.composeapp.generated.resources.lending_details_cancel_confirm_title
import cea_app.composeapp.generated.resources.lending_details_history
import cea_app.composeapp.generated.resources.logout
import cea_app.composeapp.generated.resources.memory_create
import cea_app.composeapp.generated.resources.nav_activities
import cea_app.composeapp.generated.resources.nav_home
import cea_app.composeapp.generated.resources.nav_lending
import cea_app.composeapp.generated.resources.nav_lendings
import cea_app.composeapp.generated.resources.nav_management
import cea_app.composeapp.generated.resources.nav_profile
import cea_app.composeapp.generated.resources.settings
import cea_app.composeapp.generated.resources.shopping_list_selected
import cea_app.composeapp.generated.resources.shopping_list_view
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.Department.Companion.isManagerOfAny
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItemType
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.typing.ShoppingList
import org.centrexcursionistalcoi.app.ui.composition.LocalNavigationBarVisibility
import org.centrexcursionistalcoi.app.ui.dialog.DeleteDialog
import org.centrexcursionistalcoi.app.ui.dialog.LendingsHistoryDialog
import org.centrexcursionistalcoi.app.ui.dialog.LogoutConfirmationDialog
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Add
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Face
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.FaceFilled
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.FreeCancellation
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Hiking
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.History
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Home
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.HomeFilled
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Inventory2
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Inventory2Filled
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Logout
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Package2
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Package2Filled
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Receipt
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Settings
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.SupervisorAccount
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.SupervisorAccountFilled
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Sync
import org.centrexcursionistalcoi.app.ui.page.main.ActivitiesPage
import org.centrexcursionistalcoi.app.ui.page.main.HomePage
import org.centrexcursionistalcoi.app.ui.page.main.LendingPage
import org.centrexcursionistalcoi.app.ui.page.main.LendingsPage
import org.centrexcursionistalcoi.app.ui.page.main.MANAGEMENT_PAGE_DEPARTMENTS
import org.centrexcursionistalcoi.app.ui.page.main.MANAGEMENT_PAGE_INVENTORY
import org.centrexcursionistalcoi.app.ui.page.main.MANAGEMENT_PAGE_LENDINGS
import org.centrexcursionistalcoi.app.ui.page.main.MANAGEMENT_PAGE_USERS
import org.centrexcursionistalcoi.app.ui.page.main.ManagementPage
import org.centrexcursionistalcoi.app.ui.page.main.ProfilePage
import org.centrexcursionistalcoi.app.ui.reusable.ConditionalBadge
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.ui.reusable.buttons.TooltipIconButton
import org.centrexcursionistalcoi.app.ui.utils.departmentsCountBadge
import org.centrexcursionistalcoi.app.ui.utils.lendingsCountBadge
import org.centrexcursionistalcoi.app.viewmodel.MainScreenViewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.uuid.Uuid

@Composable
fun MainScreen(
    showingAdminItemTypeId: Uuid?,
    showingAdminLendingsScreen: Boolean,
    onShoppingListConfirmed: (ShoppingList) -> Unit,
    onLendingSignUpRequested: () -> Unit,
    onLendingClick: (ReferencedLending) -> Unit,
    onMemoryEditorRequested: (ReferencedLending) -> Unit,
    onCreateMemoryRequested: () -> Unit,
    onOtherUserLendingClick: (ReferencedLending) -> Unit,
    onDeleteLendingRequest: (ReferencedLending, reason: String?) -> Job,
    onItemTypeDetailsRequested: (ReferencedInventoryItemType) -> Unit,
    onLogoutRequested: () -> Unit,
    onSettingsRequested: () -> Unit,
    model: MainScreenViewModel = koinViewModel(),
) {
    val profile by model.profile.collectAsState()
    val departments by model.departments.collectAsState()
    val lendings by model.lendings.collectAsState()
    val activeUserLending by model.activeUserLending.collectAsState()
    val isSyncing by model.isSyncing.collectAsState()

    profile?.let {
        MainScreenContent(
            showingAdminItemTypeId = showingAdminItemTypeId,
            showingAdminLendingsScreen = showingAdminLendingsScreen,
            onSettingsRequested = onSettingsRequested,
            profile = it,
            onLogoutRequested = onLogoutRequested,
            departments = departments,
            lendings = lendings,
            activeUserLending = activeUserLending,
            onLendingSignUpRequested = onLendingSignUpRequested,
            onLendingCancelRequested = model::cancelLending,
            onLendingClick = onLendingClick,
            onOtherUserLendingClick = onOtherUserLendingClick,
            onDeleteLendingRequest = onDeleteLendingRequest,
            onMemoryEditorRequested = onMemoryEditorRequested,
            onCreateMemoryRequested = onCreateMemoryRequested,
            isSyncing = isSyncing == true,
            onSyncRequested = model::sync,
            onItemTypeDetailsRequested = onItemTypeDetailsRequested,
            onShoppingListConfirmed = onShoppingListConfirmed,
        )
    } ?: LoadingBox()
}

private enum class Page {
    HOME, LENDINGS, LENDING, ACTIVITIES, MANAGEMENT, PROFILE
}

private class NavigationItem(
    val icon: ImageVector,
    val filledIcon: ImageVector,
    val label: StringResource,
) {
    @Composable
    fun Icon(isSelected: Boolean) {
        AnimatedContent(
            targetState = isSelected,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
        ) { selected ->
            Icon(
                if (selected) filledIcon else icon,
                stringResource(label)
            )
        }
    }
}

private fun navigationItems(isAdmin: Boolean, isManagerOfAnyDepartment: Boolean, anyActiveLending: Boolean): Map<Page, NavigationItem> {
    return mutableMapOf<Page, NavigationItem>().apply {
        put(
            Page.HOME,
            NavigationItem(
                icon = MaterialSymbols.Home,
                filledIcon = MaterialSymbols.HomeFilled,
                label = Res.string.nav_home
            )
        )
        if (anyActiveLending)
            put(
                Page.LENDING,
                NavigationItem(
                    icon = MaterialSymbols.Package2,
                    filledIcon = MaterialSymbols.Package2Filled,
                    label = Res.string.nav_lending
                )
            )
        else
            put(
                Page.LENDINGS,
                NavigationItem(
                    icon = MaterialSymbols.Inventory2,
                    filledIcon = MaterialSymbols.Inventory2Filled,
                    label = Res.string.nav_lendings,
                )
            )
        put(
            Page.ACTIVITIES,
            NavigationItem(
                icon = MaterialSymbols.Hiking,
                filledIcon = MaterialSymbols.Hiking,
                label = Res.string.nav_activities
            )
        )
        if (isAdmin || isManagerOfAnyDepartment) {
            put(
                Page.MANAGEMENT,
                NavigationItem(
                    icon = MaterialSymbols.SupervisorAccount,
                    filledIcon = MaterialSymbols.SupervisorAccountFilled,
                    label = Res.string.nav_management
                )
            )
        }
        put(
            Page.PROFILE,
            NavigationItem(
                icon = MaterialSymbols.Face,
                filledIcon = MaterialSymbols.FaceFilled,
                label = Res.string.nav_profile
            )
        )
    }.toMap()
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
private fun MainScreenContent(
    showingAdminItemTypeId: Uuid?,
    showingAdminLendingsScreen: Boolean,

    onSettingsRequested: () -> Unit,

    profile: ProfileResponse,
    onLogoutRequested: () -> Unit,

    departments: List<Department>?,

    lendings: List<ReferencedLending>?,
    activeUserLending: ReferencedLending?,
    onLendingSignUpRequested: () -> Unit,
    onLendingCancelRequested: (ReferencedLending) -> Job,
    onLendingClick: (ReferencedLending) -> Unit,
    onOtherUserLendingClick: (ReferencedLending) -> Unit,
    onDeleteLendingRequest: (ReferencedLending, reason: String?) -> Job,

    onMemoryEditorRequested: (ReferencedLending) -> Unit,
    onCreateMemoryRequested: () -> Unit,
    onItemTypeDetailsRequested: (ReferencedInventoryItemType) -> Unit,
    onShoppingListConfirmed: (ShoppingList) -> Unit,

    isSyncing: Boolean,
    onSyncRequested: () -> Unit
) {
    var shoppingList by remember { mutableStateOf<ShoppingList>(emptyMap()) }
    val isManagerOfAnyDepartment = remember(profile, departments) {
        departments.orEmpty().isManagerOfAny(profile)
    }
    val navigationItems = remember(profile, activeUserLending) {
        navigationItems(isAdmin = profile.isAdmin, isManagerOfAnyDepartment, anyActiveLending = activeUserLending != null)
    }

    val scope = rememberCoroutineScope()
    val pager = rememberPagerState { navigationItems.size }
    val windowSizeClass = calculateWindowSizeClass()
    val snackbarHostState = remember { SnackbarHostState() }

    val actualPage: Page = remember(pager.currentPage) {
        val pages = navigationItems.keys.toList()
        pages[pager.currentPage]
    }

    fun scrollToPage(page: Page, animated: Boolean = false) {
        scope.launch {
            val page = navigationItems.keys.indexOf(page)
            if (animated)
                pager.animateScrollToPage(page)
            else
                pager.scrollToPage(page)
        }
    }

    var showingLogoutDialog by remember { mutableStateOf(false) }
    if (showingLogoutDialog) {
        LogoutConfirmationDialog(
            onLogoutRequested = {
                showingLogoutDialog = false
                onLogoutRequested()
            },
            onDismissRequested = { showingLogoutDialog = false },
        )
    }

    var showingLendingHistory by remember { mutableStateOf(false) }
    if (showingLendingHistory) {
        LendingsHistoryDialog(
            lendings = lendings.orEmpty().filterNot { it.status().isPending() },
            onClick = {
                showingLendingHistory = false
                onLendingClick(it)
            },
            onDismissRequest = { showingLendingHistory = false }
        )
    }

    var cancellingLending by remember { mutableStateOf<ReferencedLending?>(null) }
    LaunchedEffect(lendings) {
        // Keep the cancellingLending updated
        if (cancellingLending != null) cancellingLending = lendings?.find { it.id == cancellingLending?.id }
    }
    cancellingLending?.let { lending ->
        DeleteDialog(
            title = stringResource(Res.string.lending_details_cancel_confirm_title),
            message = stringResource(Res.string.lending_details_cancel_confirm_message),
            buttonText = stringResource(Res.string.lending_details_cancel),
            onDelete = {
                onLendingCancelRequested(lending).also {
                    it.invokeOnCompletion {
                        cancellingLending = null
                        scrollToPage(Page.HOME)
                    }
                }
            },
            onDismissRequested = { cancellingLending = null }
        )
    }

    val selectedManagementItem = remember(showingAdminItemTypeId, showingAdminLendingsScreen) {
        if (showingAdminItemTypeId != null) {
            Pair(MANAGEMENT_PAGE_INVENTORY, showingAdminItemTypeId)
        } else if (showingAdminLendingsScreen) {
            Pair(MANAGEMENT_PAGE_LENDINGS, null)
        } else {
            null
        }
    }
    LaunchedEffect(profile, showingAdminItemTypeId, showingAdminLendingsScreen) {
        if (profile.isAdmin) {
            if (showingAdminItemTypeId != null) {
                pager.scrollToPage(
                    navigationItems.keys.indexOf(Page.MANAGEMENT)
                )
            } else if (showingAdminLendingsScreen) {
                pager.scrollToPage(
                    navigationItems.keys.indexOf(Page.MANAGEMENT)
                )
            }
        }
    }

    val navigationBarVisibility = remember { MutableStateFlow(true) }
    val isNavigationBarVisible by navigationBarVisibility.collectAsState()

    Scaffold(
        topBar = {
            if (windowSizeClass.widthSizeClass <= WindowWidthSizeClass.Medium) {
                AnimatedVisibility(
                    visible = isNavigationBarVisible,
                    enter = slideInVertically { -it },
                    exit = slideOutVertically { -it },
                ) {
                    TopAppBar(
                        title = { Text(stringResource(Res.string.app_name)) },
                        navigationIcon = {
                            Image(
                                painter = painterResource(Res.drawable.icon),
                                contentDescription = stringResource(Res.string.app_name),
                                modifier = Modifier.size(36.dp),
                                contentScale = ContentScale.Inside,
                            )
                        },
                        actions = {
                            if (profile.isAdmin) {
                                Badge { Text(stringResource(Res.string.admin)) }
                            }
                            when (actualPage) {
                                Page.LENDINGS, Page.LENDING -> {
                                    LendingsActionBarIcons(
                                        activeLending = activeUserLending,
                                        // Filter only the lendings owned by the logged in user
                                        lendings = lendings?.filter { it.user.sub == profile.sub },
                                        onCancelLendingRequest = { cancellingLending = activeUserLending },
                                        onLendingHistoryRequest = { showingLendingHistory = true },
                                    )
                                }

                                Page.PROFILE -> {
                                    IconButton(
                                        onClick = onSettingsRequested
                                    ) {
                                        Icon(MaterialSymbols.Settings, stringResource(Res.string.settings))
                                    }
                                    IconButton(
                                        onClick = { showingLogoutDialog = true }
                                    ) {
                                        Icon(MaterialSymbols.Logout, stringResource(Res.string.logout))
                                    }
                                }

                                else -> {}
                            }
                        }
                    )
                }
            }
        },
        bottomBar = {
            if (windowSizeClass.widthSizeClass <= WindowWidthSizeClass.Medium) {
                AnimatedVisibility(
                    visible = isNavigationBarVisible,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it },
                ) {
                    NavigationBar {
                        for ((index, entry) in navigationItems.entries.withIndex()) {
                            val (page, item) = entry
                            val isSelected = pager.currentPage == index
                            val badgeText = if (page == Page.MANAGEMENT) {
                                val departmentsCount = departments.departmentsCountBadge() ?: 0
                                val lendingsCount = lendings.lendingsCountBadge() ?: 0
                                (departmentsCount + lendingsCount).takeIf { it > 0 }?.toString()
                            } else null
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { scope.launch { pager.animateScrollToPage(index) } },
                                label = { Text(stringResource(item.label)) },
                                icon = {
                                    ConditionalBadge(badgeText) {
                                        item.Icon(isSelected)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = actualPage == Page.LENDINGS && shoppingList.isNotEmpty(),
                enter = slideInHorizontally { it },
                exit = slideOutHorizontally { it },
            ) {
                ExtendedFloatingActionButton(
                    onClick = { onShoppingListConfirmed(shoppingList) },
                ) {
                    Icon(
                        imageVector = MaterialSymbols.Receipt,
                        contentDescription = stringResource(Res.string.shopping_list_view)
                    )
                    Spacer(Modifier.padding(4.dp))
                    Text(
                        stringResource(
                            Res.string.shopping_list_selected,
                            shoppingList.toList().sumOf { it.second }
                        )
                    )
                }
            }
            AnimatedVisibility(
                visible = actualPage == Page.ACTIVITIES,
                enter = slideInHorizontally { it },
                exit = slideOutHorizontally { it },
            ) {
                FloatingActionButton(
                    onClick = onCreateMemoryRequested,
                ) {
                    Icon(
                        imageVector = MaterialSymbols.Add,
                        contentDescription = stringResource(Res.string.memory_create)
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Row(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            @Composable
            fun Content(pageIdx: Int) {
                val entry = navigationItems.entries.toList()[pageIdx]
                MainScreenPagerContent(
                    page = entry.key,
                    onPageRequested = ::scrollToPage,
                    snackbarHostState = snackbarHostState,
                    selectedManagementItem = selectedManagementItem,
                    profile = profile,
                    departments = departments,
                    activeLending = activeUserLending,
                    onLendingSignUpRequested = onLendingSignUpRequested,
                    onCancelLendingRequest = { cancellingLending = it },
                    onLendingHistoryRequest = { showingLendingHistory = true },
                    onItemTypeDetailsRequested = onItemTypeDetailsRequested,
                    onOtherUserLendingClick = onOtherUserLendingClick,
                    onDeleteLendingRequest = onDeleteLendingRequest,
                    onShoppingListChanged = { shoppingList = it },
                    onMemoryEditorRequested = onMemoryEditorRequested
                )
            }

            CompositionLocalProvider(LocalNavigationBarVisibility provides navigationBarVisibility) {
                if (windowSizeClass.widthSizeClass > WindowWidthSizeClass.Medium) {
                    NavigationRail(
                        header = {
                            Image(
                                painter = painterResource(Res.drawable.icon),
                                contentDescription = stringResource(Res.string.app_name),
                                modifier = Modifier.size(36.dp),
                                contentScale = ContentScale.Inside,
                            )
                        }
                    ) {
                        NavigationRailItems(
                            pager = pager,
                            navigationItems = navigationItems,
                            isAdmin = profile.isAdmin,
                            isSyncing = isSyncing,
                            departments = departments,
                            lendings = lendings,
                            onSettingsRequested = onSettingsRequested,
                            onLogoutRequested = { showingLogoutDialog = true },
                            onSyncRequested = onSyncRequested,
                        )
                    }

                    VerticalPager(
                        state = pager,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = false
                    ) { pageIdx -> Content(pageIdx) }
                } else {
                    PullToRefreshBox(
                        isRefreshing = isSyncing,
                        onRefresh = onSyncRequested,
                    ) {
                        HorizontalPager(
                            state = pager,
                            modifier = Modifier.fillMaxSize()
                        ) { pageIdx -> Content(pageIdx) }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun LendingsActionBarIcons(
    activeLending: ReferencedLending?,
    lendings: List<ReferencedLending>?,
    onCancelLendingRequest: (ReferencedLending) -> Unit,
    onLendingHistoryRequest: () -> Unit,
) {
    // This will only be displayed on LENDING, because activeUserLending will be null on LENDINGS
    if (activeLending?.status()?.canBeCancelled() == true) {
        TooltipIconButton(
            MaterialSymbols.FreeCancellation,
            stringResource(Res.string.lending_details_cancel),
            onClick = { onCancelLendingRequest(activeLending) },
        )
    }
    // Only show history button if there are more than 1 lending in the history
    // This is, if size is greater to 1: it doesn't matter if there's an active lending, there are lendings in the history
    // or if size is 1, but there's no active lending: the user has made a lending that has already been completed
    if (lendings.orEmpty().size > 1 || (lendings.orEmpty().size == 1 && activeLending == null)) {
        TooltipIconButton(
            MaterialSymbols.History,
            stringResource(Res.string.lending_details_history),
            onClick = onLendingHistoryRequest,
        )
    }
}

@Composable
private fun MainScreenPagerContent(
    page: Page,
    onPageRequested: (Page) -> Unit,
    snackbarHostState: SnackbarHostState,

    /**
     * The currently selected management item in the format Pair(pageIndex, itemId).
     *
     * Pages: [MANAGEMENT_PAGE_LENDINGS], [MANAGEMENT_PAGE_DEPARTMENTS], [MANAGEMENT_PAGE_USERS], [MANAGEMENT_PAGE_INVENTORY].
     */
    selectedManagementItem: Pair<Int, Uuid?>?,

    profile: ProfileResponse,

    departments: List<Department>?,

    activeLending: ReferencedLending?,
    onLendingSignUpRequested: () -> Unit,
    onCancelLendingRequest: (ReferencedLending) -> Unit,
    onLendingHistoryRequest: () -> Unit,
    onMemoryEditorRequested: (ReferencedLending) -> Unit,
    onOtherUserLendingClick: (ReferencedLending) -> Unit,
    onDeleteLendingRequest: (ReferencedLending, reason: String?) -> Job,
    onItemTypeDetailsRequested: (ReferencedInventoryItemType) -> Unit,
    onShoppingListChanged: (ShoppingList) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        when (page) {
            Page.HOME -> HomePage()

            Page.LENDINGS -> LendingsPage(
                onAddInsuranceRequested = { onPageRequested(Page.PROFILE) },
                onItemTypeDetailsRequested,
                onLendingSignUpRequested,
                onLendingHistoryRequest,
                onShoppingListChanged = onShoppingListChanged,
            )

            Page.LENDING if activeLending != null -> LendingPage(
                onCancelLendingRequest = onCancelLendingRequest,
                onLendingHistoryRequest = onLendingHistoryRequest,
                onMemoryEditorRequested = onMemoryEditorRequested,
            )
            // If lending is selected, but there's no active lending, move to home
            Page.LENDING -> LaunchedEffect(Unit) { onPageRequested(Page.HOME) }

            Page.ACTIVITIES -> ActivitiesPage()

            // Management page only for admins or department managers
            Page.MANAGEMENT if (profile.isAdmin || departments.orEmpty().isManagerOfAny(profile)) -> ManagementPage(
                snackbarHostState = snackbarHostState,
                selectedItem = selectedManagementItem,
                onGiveRequested = onOtherUserLendingClick,
                onReceiveRequested = onOtherUserLendingClick,
                onDeleteRequested = onDeleteLendingRequest,
            )

            Page.MANAGEMENT -> Text(stringResource(Res.string.error_access_denied))

            Page.PROFILE -> ProfilePage()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.NavigationRailItems(
    pager: PagerState,
    navigationItems: Map<Page, NavigationItem>,
    isAdmin: Boolean,
    isSyncing: Boolean,
    departments: List<Department>?,
    lendings: List<ReferencedLending>?,
    onSettingsRequested: () -> Unit,
    onLogoutRequested: () -> Unit,
    onSyncRequested: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    if (isAdmin) {
        Badge(
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 12.dp),
        ) { Text(stringResource(Res.string.admin)) }
    }

    for ((index, entry) in navigationItems.entries.withIndex()) {
        val (page, item) = entry
        val isSelected = pager.currentPage == index
        val badgeText = if (page == Page.MANAGEMENT) {
            val departmentsCount = departments.departmentsCountBadge() ?: 0
            val lendingsCount = lendings.lendingsCountBadge() ?: 0
            (departmentsCount + lendingsCount).takeIf { it > 0 }?.toString()
        } else null
        NavigationRailItem(
            selected = isSelected,
            onClick = { scope.launch { pager.animateScrollToPage(index) } },
            label = { Text(stringResource(item.label)) },
            icon = {
                ConditionalBadge(badgeText) {
                    item.Icon(isSelected)
                }
            }
        )
    }
    Spacer(Modifier.weight(1f))
    TooltipBox(
        state = rememberTooltipState(),
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Right),
        tooltip = {
            PlainTooltip { Text(stringResource(Res.string.settings)) }
        }
    ) {
        NavigationRailItem(
            selected = false,
            onClick = onSettingsRequested,
            icon = { Icon(MaterialSymbols.Settings, stringResource(Res.string.settings)) }
        )
    }
    TooltipBox(
        state = rememberTooltipState(),
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Right),
        tooltip = {
            PlainTooltip { Text(stringResource(Res.string.logout)) }
        }
    ) {
        NavigationRailItem(
            selected = false,
            onClick = onLogoutRequested,
            icon = { Icon(MaterialSymbols.Logout, stringResource(Res.string.logout)) }
        )
    }
    TooltipBox(
        state = rememberTooltipState(),
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Right),
        tooltip = {
            PlainTooltip { Text(stringResource(Res.string.force_sync)) }
        }
    ) {
        NavigationRailItem(
            selected = false,
            enabled = !isSyncing,
            onClick = onSyncRequested,
            icon = { Icon(MaterialSymbols.Sync, stringResource(Res.string.force_sync)) }
        )
    }
}
