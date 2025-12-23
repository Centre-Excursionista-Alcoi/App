package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.data.*
import org.centrexcursionistalcoi.app.permission.result.NotificationPermissionResult
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.typing.ShoppingList
import org.centrexcursionistalcoi.app.ui.composition.LocalNavigationBarVisibility
import org.centrexcursionistalcoi.app.ui.dialog.CreateInsuranceRequest
import org.centrexcursionistalcoi.app.ui.dialog.DeleteDialog
import org.centrexcursionistalcoi.app.ui.dialog.LendingsHistoryDialog
import org.centrexcursionistalcoi.app.ui.dialog.LogoutConfirmationDialog
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.*
import org.centrexcursionistalcoi.app.ui.page.main.*
import org.centrexcursionistalcoi.app.ui.platform.calculateWindowSizeClass
import org.centrexcursionistalcoi.app.ui.reusable.ConditionalBadge
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.ui.reusable.buttons.TooltipIconButton
import org.centrexcursionistalcoi.app.ui.utils.departmentsCountBadge
import org.centrexcursionistalcoi.app.ui.utils.lendingsCountBadge
import org.centrexcursionistalcoi.app.viewmodel.MainViewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.Uuid

@Composable
fun MainScreen(
    showingAdminItemTypeId: Uuid?,
    showingAdminLendingsScreen: Boolean,
    onShoppingListConfirmed: (ShoppingList) -> Unit,
    onLendingSignUpRequested: () -> Unit,
    onLendingClick: (ReferencedLending) -> Unit,
    onMemoryEditorRequested: (ReferencedLending) -> Unit,
    onOtherUserLendingClick: (ReferencedLending) -> Unit,
    onItemTypeDetailsRequested: (ReferencedInventoryItemType) -> Unit,
    onLogoutRequested: () -> Unit,
    onSettingsRequested: () -> Unit,
    model: MainViewModel = viewModel { MainViewModel() }
) {
    val profile by model.profile.collectAsState()
    val departments by model.departments.collectAsState()
    val users by model.users.collectAsState()
    val members by model.members.collectAsState()
    val inventoryItemTypes by model.inventoryItemTypes.collectAsState()
    val inventoryItemTypesCategories by model.inventoryItemTypesCategories.collectAsState()
    val inventoryItems by model.inventoryItems.collectAsState()
    val lendings by model.lendings.collectAsState()
    val posts by model.posts.collectAsState()
    val events by model.events.collectAsState()
    val isSyncing by model.isSyncing.collectAsState()
    val shoppingList by model.shoppingList.collectAsState()
    val notificationPermissionResult by model.notificationPermissionResult.collectAsState()

    LifecycleResumeEffect(model) {
        model.refreshPermissions()
        onPauseOrDispose { /* nothing */ }
    }

    profile?.let {
        MainScreenContent(
            showingAdminItemTypeId = showingAdminItemTypeId,
            showingAdminLendingsScreen = showingAdminLendingsScreen,
            notificationPermissionResult = notificationPermissionResult,
            onNotificationPermissionRequest = model::requestNotificationsPermission,
            onNotificationPermissionDenyRequest = model::denyNotificationsPermission,
            onSettingsRequested = onSettingsRequested,
            profile = it,
            onLogoutRequested = onLogoutRequested,
            departments = departments,
            onJoinDepartmentRequested = model::requestJoinDepartment,
            onLeaveDepartmentRequested = model::leaveDepartment,
            lendings = lendings,
            onLendingSignUpRequested = onLendingSignUpRequested,
            onLendingCancelRequested = model::cancelLending,
            onLendingClick = onLendingClick,
            onOtherUserLendingClick = onOtherUserLendingClick,
            onMemoryEditorRequested = onMemoryEditorRequested,
            onCreateInsurance = model::createInsurance,
            onFEMECVConnectRequested = model::connectFEMECV,
            onFEMECVDisconnectRequested = model::disconnectFEMECV,
            users = users,
            members = members,
            isSyncing = isSyncing == true,
            onSyncRequested = model::sync,
            inventoryItemTypes = inventoryItemTypes,
            inventoryItemTypesCategories = inventoryItemTypesCategories.orEmpty(),
            onItemTypeDetailsRequested = onItemTypeDetailsRequested,
            inventoryItems = inventoryItems,
            posts = posts,
            events = events,
            onConfirmAssistanceRequest = model::confirmEventAssistance,
            onRejectAssistanceRequest = model::rejectEventAssistance,
            shoppingList = shoppingList,
            onAddItemToShoppingListRequest = model::addItemToShoppingList,
            onRemoveItemFromShoppingListRequest = model::removeItemFromShoppingList,
            onShoppingListConfirmed = { onShoppingListConfirmed(shoppingList) },
        )
    } ?: LoadingBox()
}

private enum class Page {
    HOME, LENDINGS, LENDING, MANAGEMENT, PROFILE
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

private fun navigationItems(isAdmin: Boolean, anyActiveLending: Boolean): Map<Page, NavigationItem> {
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
        if (isAdmin) {
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
@OptIn(ExperimentalMaterial3Api::class)
private fun MainScreenContent(
    showingAdminItemTypeId: Uuid?,
    showingAdminLendingsScreen: Boolean,

    notificationPermissionResult: NotificationPermissionResult?,
    onNotificationPermissionRequest: () -> Unit,
    onNotificationPermissionDenyRequest: () -> Unit,

    onSettingsRequested: () -> Unit,

    profile: ProfileResponse,
    onLogoutRequested: () -> Unit,

    departments: List<Department>?,
    onJoinDepartmentRequested: (Department) -> Job,
    onLeaveDepartmentRequested: (Department) -> Job,

    lendings: List<ReferencedLending>?,
    onLendingSignUpRequested: () -> Unit,
    onLendingCancelRequested: (ReferencedLending) -> Job,
    onLendingClick: (ReferencedLending) -> Unit,
    onOtherUserLendingClick: (ReferencedLending) -> Unit,
    onMemoryEditorRequested: (ReferencedLending) -> Unit,

    onCreateInsurance: CreateInsuranceRequest,
    onFEMECVConnectRequested: (username: String, password: CharArray) -> Deferred<Throwable?>,
    onFEMECVDisconnectRequested: () -> Job,

    users: List<UserData>?,

    members: List<Member>?,

    inventoryItemTypes: List<ReferencedInventoryItemType>?,
    inventoryItemTypesCategories: Set<String>,
    onItemTypeDetailsRequested: (ReferencedInventoryItemType) -> Unit,

    inventoryItems: List<ReferencedInventoryItem>?,

    posts: List<ReferencedPost>?,

    events: List<ReferencedEvent>?,
    onConfirmAssistanceRequest: (ReferencedEvent) -> Job,
    onRejectAssistanceRequest: (ReferencedEvent) -> Job,

    shoppingList: ShoppingList,
    onAddItemToShoppingListRequest: (ReferencedInventoryItemType) -> Unit,
    onRemoveItemFromShoppingListRequest: (ReferencedInventoryItemType) -> Unit,
    onShoppingListConfirmed: () -> Unit,

    isSyncing: Boolean,
    onSyncRequested: () -> Unit
) {
    val activeUserLending = lendings
        // Get only lendings of the current user
        ?.filter { it.user.sub == profile.sub || it.user.isStub() }
        // Find the pending lending
        ?.find { it.status().isPending() }
    val navigationItems = remember(profile, activeUserLending) {
        navigationItems(isAdmin = profile.isAdmin, anyActiveLending = activeUserLending != null)
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
                                        lendings = lendings,
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
                    onClick = onShoppingListConfirmed,
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
                    snackbarHostState,
                    selectedManagementItem,
                    notificationPermissionResult,
                    onNotificationPermissionRequest,
                    onNotificationPermissionDenyRequest,
                    profile,
                    { scrollToPage(Page.PROFILE, true) },
                    windowSizeClass,
                    departments,
                    onJoinDepartmentRequested,
                    onLeaveDepartmentRequested,
                    lendings,
                    activeUserLending,
                    onLendingSignUpRequested,
                    onOtherUserLendingClick,
                    { cancellingLending = it },
                    { showingLendingHistory = true },
                    onMemoryEditorRequested,
                    onCreateInsurance,
                    onFEMECVConnectRequested,
                    onFEMECVDisconnectRequested,
                    users,
                    members,
                    inventoryItemTypes,
                    inventoryItemTypesCategories,
                    onItemTypeDetailsRequested,
                    inventoryItems,
                    posts,
                    events,
                    onConfirmAssistanceRequest,
                    onRejectAssistanceRequest,
                    shoppingList,
                    onAddItemToShoppingListRequest,
                    onRemoveItemFromShoppingListRequest,
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

    notificationPermissionResult: NotificationPermissionResult?,
    onNotificationPermissionRequest: () -> Unit,
    onNotificationPermissionDenyRequest: () -> Unit,

    profile: ProfileResponse,
    onAddInsuranceRequested: () -> Unit,

    windowSizeClass: WindowSizeClass,

    departments: List<Department>?,
    onJoinDepartmentRequested: (Department) -> Job,
    onLeaveDepartmentRequested: (Department) -> Job,

    lendings: List<ReferencedLending>?,
    activeLending: ReferencedLending?,
    onLendingSignUpRequested: () -> Unit,
    onOtherUserLendingClick: (ReferencedLending) -> Unit,
    onCancelLendingRequest: (ReferencedLending) -> Unit,
    onLendingHistoryRequest: () -> Unit,
    onMemoryEditorRequested: (ReferencedLending) -> Unit,

    onCreateInsurance: CreateInsuranceRequest,
    onFEMECVConnectRequested: (username: String, password: CharArray) -> Deferred<Throwable?>,
    onFEMECVDisconnectRequested: () -> Job,

    users: List<UserData>?,

    members: List<Member>?,

    inventoryItemTypes: List<ReferencedInventoryItemType>?,
    inventoryItemTypesCategories: Set<String>,
    onItemTypeDetailsRequested: (ReferencedInventoryItemType) -> Unit,

    inventoryItems: List<ReferencedInventoryItem>?,

    posts: List<ReferencedPost>?,

    events: List<ReferencedEvent>?,
    onConfirmAssistanceRequest: (ReferencedEvent) -> Job,
    onRejectAssistanceRequest: (ReferencedEvent) -> Job,

    shoppingList: Map<Uuid, Int>,
    onAddItemToShoppingListRequest: (ReferencedInventoryItemType) -> Unit,
    onRemoveItemFromShoppingListRequest: (ReferencedInventoryItemType) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        when (page) {
            Page.HOME -> HomePage(
                windowSizeClass,
                notificationPermissionResult,
                onNotificationPermissionRequest,
                onNotificationPermissionDenyRequest,
                profile,
                posts,
                events,
                onConfirmAssistanceRequest,
                onRejectAssistanceRequest,
            )

            Page.LENDINGS -> LendingsPage(
                windowSizeClass,
                profile,
                onAddInsuranceRequested,
                inventoryItems,
                onItemTypeDetailsRequested,
                lendings,
                onLendingSignUpRequested,
                onLendingHistoryRequest,
                shoppingList,
                onAddItemToShoppingListRequest,
                onRemoveItemFromShoppingListRequest,
            )

            Page.LENDING if activeLending != null -> LendingPage(
                windowSizeClass = windowSizeClass,
                lending = activeLending,
                lendings = lendings,
                onCancelLendingRequest = onCancelLendingRequest,
                onLendingHistoryRequest = onLendingHistoryRequest,
                onMemoryEditorRequested = { onMemoryEditorRequested(activeLending) },
            )
            // If lending is selected, but there's no active lending, move to home
            Page.LENDING -> LaunchedEffect(Unit) { onPageRequested(Page.HOME) }

            Page.MANAGEMENT if profile.isAdmin -> ManagementPage(
                windowSizeClass,
                snackbarHostState,
                selectedManagementItem,
                lendings,
                onOtherUserLendingClick,
                onOtherUserLendingClick,
                departments,
                users,
                members,
                inventoryItemTypes,
                inventoryItemTypesCategories,
                inventoryItems,
                posts,
                events,
            )

            Page.MANAGEMENT -> Text(stringResource(Res.string.error_access_denied))

            Page.PROFILE -> ProfilePage(
                windowSizeClass,
                profile,
                onCreateInsurance,
                onFEMECVConnectRequested,
                onFEMECVDisconnectRequested,
                departments,
                onJoinDepartmentRequested,
                onLeaveDepartmentRequested,
            )
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
