package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.*
import io.github.vinceglb.filekit.PlatformFile
import kotlin.uuid.Uuid
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.permission.result.NotificationPermissionResult
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.typing.ShoppingList
import org.centrexcursionistalcoi.app.ui.dialog.CreateInsuranceRequest
import org.centrexcursionistalcoi.app.ui.dialog.LogoutConfirmationDialog
import org.centrexcursionistalcoi.app.ui.page.home.HomePage
import org.centrexcursionistalcoi.app.ui.page.home.LendingsPage
import org.centrexcursionistalcoi.app.ui.page.home.ManagementPage
import org.centrexcursionistalcoi.app.ui.page.home.ProfilePage
import org.centrexcursionistalcoi.app.ui.platform.calculateWindowSizeClass
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.viewmodel.HomeViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun MainScreen(
    onClickInventoryItemType: (InventoryItemType) -> Unit,
    onManageLendingsRequested: () -> Unit,
    onShoppingListConfirmed: (ShoppingList) -> Unit,
    onLendingSignUpRequested: () -> Unit,
    onLendingClick: (ReferencedLending) -> Unit,
    onOtherUserLendingClick: (ReferencedLending) -> Unit,
    onItemTypeDetailsRequested: (InventoryItemType) -> Unit,
    onLogoutRequested: () -> Unit,
    onSettingsRequested: () -> Unit,
    model: HomeViewModel = viewModel { HomeViewModel() }
) {
    val profile by model.profile.collectAsState()
    val departments by model.departments.collectAsState()
    val users by model.users.collectAsState()
    val inventoryItemTypes by model.inventoryItemTypes.collectAsState()
    val inventoryItemTypesCategories by model.inventoryItemTypesCategories.collectAsState()
    val inventoryItems by model.inventoryItems.collectAsState()
    val lendings by model.lendings.collectAsState()
    val isSyncing by model.isSyncing.collectAsState()
    val shoppingList by model.shoppingList.collectAsState()
    val notificationPermissionResult by model.notificationPermissionResult.collectAsState()

    LifecycleResumeEffect(model) {
        model.refreshPermissions()
        onPauseOrDispose { /* nothing */ }
    }

    profile?.let {
        MainScreenContent(
            notificationPermissionResult = notificationPermissionResult,
            onNotificationPermissionRequest = model::requestNotificationsPermission,
            onNotificationPermissionDenyRequest = model::denyNotificationsPermission,
            onSettingsRequested = onSettingsRequested,
            profile = it,
            onLogoutRequested = onLogoutRequested,
            departments = departments,
            onCreateDepartment = model::createDepartment,
            onDeleteDepartment = model::delete,
            lendings = lendings,
            onLendingSignUpRequested = onLendingSignUpRequested,
            onLendingClick = onLendingClick,
            onOtherUserLendingClick = onOtherUserLendingClick,
            onCreateInsurance = model::createInsurance,
            onFEMECVConnectRequested = model::connectFEMECV,
            onFEMECVDisconnectRequested = model::disconnectFEMECV,
            users = users,
            onPromote = model::promote,
            isSyncing = isSyncing == true,
            onSyncRequested = model::sync,
            inventoryItemTypes = inventoryItemTypes,
            inventoryItemTypesCategories = inventoryItemTypesCategories.orEmpty(),
            onItemTypeDetailsRequested = onItemTypeDetailsRequested,
            onCreateInventoryItemType = model::createInventoryItemType,
            onClickInventoryItemType = onClickInventoryItemType,
            inventoryItems = inventoryItems,
            onManageLendingsRequested = onManageLendingsRequested,
            shoppingList = shoppingList,
            onAddItemToShoppingListRequest = model::addItemToShoppingList,
            onRemoveItemFromShoppingListRequest = model::removeItemFromShoppingList,
            onShoppingListConfirmed = { onShoppingListConfirmed(shoppingList) },
        )
    } ?: LoadingBox()
}

private enum class Page {
    HOME, LENDINGS, MANAGEMENT, PROFILE
}

private typealias NavigationItem = Pair<ImageVector, @Composable (() -> String)>

private fun navigationItems(isAdmin: Boolean, anyActiveLending: Boolean): Map<Page, NavigationItem> {
    return mutableMapOf<Page, NavigationItem>().apply {
        put(Page.HOME, Icons.Default.Home to { stringResource(Res.string.nav_home) })
        if (!anyActiveLending) {
            put(Page.LENDINGS, Icons.Default.Inventory2 to { stringResource(Res.string.nav_lendings) })
        }
        if (isAdmin) {
            put(Page.MANAGEMENT, Icons.Default.SupervisorAccount to { stringResource(Res.string.nav_management) })
        }
        put(Page.PROFILE, Icons.Default.Face to { stringResource(Res.string.nav_profile) })
    }.toMap()
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MainScreenContent(
    notificationPermissionResult: NotificationPermissionResult?,
    onNotificationPermissionRequest: () -> Unit,
    onNotificationPermissionDenyRequest: () -> Unit,

    onSettingsRequested: () -> Unit,

    profile: ProfileResponse,
    onLogoutRequested: () -> Unit,

    departments: List<Department>?,
    onCreateDepartment: (displayName: String, image: PlatformFile?) -> Job,
    onDeleteDepartment: (Department) -> Job,

    lendings: List<ReferencedLending>?,
    onLendingSignUpRequested: () -> Unit,
    onLendingClick: (ReferencedLending) -> Unit,
    onOtherUserLendingClick: (ReferencedLending) -> Unit,

    onCreateInsurance: CreateInsuranceRequest,
    onFEMECVConnectRequested: (username: String, password: CharArray) -> Deferred<Throwable?>,
    onFEMECVDisconnectRequested: () -> Job,

    users: List<UserData>?,
    onPromote: (UserData) -> Job,

    inventoryItemTypes: List<InventoryItemType>?,
    inventoryItemTypesCategories: Set<String>,
    onItemTypeDetailsRequested: (InventoryItemType) -> Unit,
    onCreateInventoryItemType: (displayName: String, description: String, categories: List<String>, image: PlatformFile?) -> Job,
    onClickInventoryItemType: (InventoryItemType) -> Unit,

    inventoryItems: List<ReferencedInventoryItem>?,

    onManageLendingsRequested: () -> Unit,

    shoppingList: ShoppingList,
    onAddItemToShoppingListRequest: (InventoryItemType) -> Unit,
    onRemoveItemFromShoppingListRequest: (InventoryItemType) -> Unit,
    onShoppingListConfirmed: () -> Unit,

    isSyncing: Boolean,
    onSyncRequested: () -> Unit
) {
    val activeLendingsCount = lendings?.count { it.status() !in listOf(Lending.Status.MEMORY_SUBMITTED, Lending.Status.COMPLETE) } ?: 0
    val navigationItems = navigationItems(isAdmin = profile.isAdmin, anyActiveLending = activeLendingsCount > 0)

    val scope = rememberCoroutineScope()
    val pager = rememberPagerState { navigationItems.size }
    val windowSizeClass = calculateWindowSizeClass()
    val snackbarHostState = remember { SnackbarHostState() }

    val actualPage: Page = remember(pager.currentPage) {
        val pages = navigationItems.keys.toList()
        pages[pager.currentPage]
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

    Scaffold(
        topBar = {
            if (windowSizeClass.widthSizeClass <= WindowWidthSizeClass.Medium) {
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
                        if (actualPage == Page.PROFILE) {
                            IconButton(
                                onClick = onSettingsRequested
                            ) {
                                Icon(Icons.Default.Settings, stringResource(Res.string.settings))
                            }
                            IconButton(
                                onClick = { showingLogoutDialog = true }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Logout, stringResource(Res.string.logout))
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (windowSizeClass.widthSizeClass <= WindowWidthSizeClass.Medium) {
                NavigationBar {
                    for ((index, item) in navigationItems.values.withIndex()) {
                        val (icon, label) = item
                        NavigationBarItem(
                            selected = pager.currentPage == index,
                            onClick = { scope.launch { pager.animateScrollToPage(index) } },
                            label = { Text(label()) },
                            icon = { Icon(icon, label()) }
                        )
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
                        imageVector = Icons.Default.Receipt,
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
                        onSettingsRequested = onSettingsRequested,
                        onLogoutRequested = { showingLogoutDialog = true },
                        onSyncRequested = onSyncRequested,
                    )
                }

                VerticalPager(
                    state = pager,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = false
                ) { pageIdx ->
                    val page = navigationItems.keys.toList()[pageIdx]
                    MainScreenPagerContent(
                        page,
                        snackbarHostState,
                        notificationPermissionResult,
                        onNotificationPermissionRequest,
                        onNotificationPermissionDenyRequest,
                        profile,
                        windowSizeClass,
                        departments,
                        onCreateDepartment,
                        onDeleteDepartment,
                        lendings,
                        onLendingSignUpRequested,
                        onLendingClick,
                        onOtherUserLendingClick,
                        onCreateInsurance,
                        onFEMECVConnectRequested,
                        onFEMECVDisconnectRequested,
                        users,
                        onPromote,
                        inventoryItemTypes,
                        inventoryItemTypesCategories,
                        onItemTypeDetailsRequested,
                        onCreateInventoryItemType,
                        onClickInventoryItemType,
                        inventoryItems,
                        shoppingList,
                        onAddItemToShoppingListRequest,
                        onRemoveItemFromShoppingListRequest,
                        onManageLendingsRequested,
                    )
                }
            } else {
                PullToRefreshBox(
                    isRefreshing = isSyncing,
                    onRefresh = onSyncRequested,
                ) {
                    HorizontalPager(
                        state = pager,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIdx ->
                        val page = navigationItems.keys.toList()[pageIdx]
                        MainScreenPagerContent(
                            page,
                            snackbarHostState,
                            notificationPermissionResult,
                            onNotificationPermissionRequest,
                            onNotificationPermissionDenyRequest,
                            profile,
                            windowSizeClass,
                            departments,
                            onCreateDepartment,
                            onDeleteDepartment,
                            lendings,
                            onLendingSignUpRequested,
                            onLendingClick,
                            onOtherUserLendingClick,
                            onCreateInsurance,
                            onFEMECVConnectRequested,
                            onFEMECVDisconnectRequested,
                            users,
                            onPromote,
                            inventoryItemTypes,
                            inventoryItemTypesCategories,
                            onItemTypeDetailsRequested,
                            onCreateInventoryItemType,
                            onClickInventoryItemType,
                            inventoryItems,
                            shoppingList,
                            onAddItemToShoppingListRequest,
                            onRemoveItemFromShoppingListRequest,
                            onManageLendingsRequested,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MainScreenPagerContent(
    page: Page,
    snackbarHostState: SnackbarHostState,
    notificationPermissionResult: NotificationPermissionResult?,
    onNotificationPermissionRequest: () -> Unit,
    onNotificationPermissionDenyRequest: () -> Unit,
    profile: ProfileResponse,
    windowSizeClass: WindowSizeClass,

    departments: List<Department>?,
    onCreateDepartment: (displayName: String, image: PlatformFile?) -> Job,
    onDeleteDepartment: (Department) -> Job,

    lendings: List<ReferencedLending>?,
    onLendingSignUpRequested: () -> Unit,
    onLendingClick: (ReferencedLending) -> Unit,
    onOtherUserLendingClick: (ReferencedLending) -> Unit,

    onCreateInsurance: CreateInsuranceRequest,
    onFEMECVConnectRequested: (username: String, password: CharArray) -> Deferred<Throwable?>,
    onFEMECVDisconnectRequested: () -> Job,

    users: List<UserData>?,
    onPromote: (UserData) -> Job,

    inventoryItemTypes: List<InventoryItemType>?,
    inventoryItemTypesCategories: Set<String>,
    onItemTypeDetailsRequested: (InventoryItemType) -> Unit,
    onCreateInventoryItemType: (displayName: String, description: String, categories: List<String>, image: PlatformFile?) -> Job,
    onClickInventoryItemType: (InventoryItemType) -> Unit,

    inventoryItems: List<ReferencedInventoryItem>?,

    shoppingList: Map<Uuid, Int>,
    onAddItemToShoppingListRequest: (InventoryItemType) -> Unit,
    onRemoveItemFromShoppingListRequest: (InventoryItemType) -> Unit,

    onManageLendingsRequested: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        when (page) {
            Page.HOME -> HomePage(
                windowSizeClass,
                notificationPermissionResult,
                onNotificationPermissionRequest,
                onNotificationPermissionDenyRequest,
                profile,
                lendings,
                onLendingClick,
                onOtherUserLendingClick,
            )

            Page.LENDINGS -> LendingsPage(
                windowSizeClass,
                profile,
                inventoryItems,
                onItemTypeDetailsRequested,
                lendings,
                onLendingSignUpRequested,
                shoppingList,
                onAddItemToShoppingListRequest,
                onRemoveItemFromShoppingListRequest,
            )

            Page.MANAGEMENT if profile.isAdmin -> ManagementPage(
                windowSizeClass,
                departments,
                onCreateDepartment,
                onDeleteDepartment,
                users,
                onPromote,
                inventoryItemTypes,
                inventoryItemTypesCategories,
                onCreateInventoryItemType,
                onClickInventoryItemType,
                inventoryItems,
                onManageLendingsRequested,
            )
            Page.MANAGEMENT -> Text(stringResource(Res.string.error_access_denied))

            Page.PROFILE -> ProfilePage(
                windowSizeClass,
                profile,
                onCreateInsurance,
                onFEMECVConnectRequested,
                onFEMECVDisconnectRequested,
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

    for ((index, item) in navigationItems.values.withIndex()) {
        val (icon, label) = item
        NavigationRailItem(
            selected = pager.currentPage == index,
            onClick = { scope.launch { pager.animateScrollToPage(index) } },
            label = { Text(label()) },
            icon = { Icon(icon, label()) }
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
            icon = { Icon(Icons.Default.Settings, stringResource(Res.string.settings)) }
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
            icon = { Icon(Icons.AutoMirrored.Default.Logout, stringResource(Res.string.logout)) }
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
            icon = { Icon(Icons.Default.Sync, stringResource(Res.string.force_sync)) }
        )
    }
}
