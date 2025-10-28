package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.typing.ShoppingList
import org.centrexcursionistalcoi.app.ui.dialog.CreateInsuranceRequest
import org.centrexcursionistalcoi.app.ui.dialog.LogoutConfirmationDialog
import org.centrexcursionistalcoi.app.ui.dialog.ShoppingListDialog
import org.centrexcursionistalcoi.app.ui.page.home.HomeMainPage
import org.centrexcursionistalcoi.app.ui.page.home.ManagementPage
import org.centrexcursionistalcoi.app.ui.page.home.ProfilePage
import org.centrexcursionistalcoi.app.ui.platform.calculateWindowSizeClass
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.viewmodel.HomeViewModel
import org.jetbrains.compose.resources.stringResource
import tech.kotlinlang.permission.result.NotificationPermissionResult

@Composable
fun HomeScreen(
    onClickInventoryItemType: (InventoryItemType) -> Unit,
    onManageLendingsRequested: () -> Unit,
    onShoppingListConfirmed: (ShoppingList) -> Unit,
    onLendingSignUpRequested: () -> Unit,
    onMemoryEditorRequested: (ReferencedLending) -> Unit,
    onLogoutRequested: () -> Unit,
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
    val memoryUploadProgress by model.memoryUploadProgress.collectAsState()
    val notificationPermissionResult by model.notificationPermissionResult.collectAsState()

    LifecycleResumeEffect(model) {
        model.refreshPermissions()
        onPauseOrDispose { /* nothing */ }
    }

    profile?.let {
        HomeScreenContent(
            notificationPermissionResult = notificationPermissionResult,
            onNotificationPermissionRequest = model::requestNotificationsPermission,
            onNotificationPermissionDenyRequest = model::denyNotificationsPermission,
            profile = it,
            onLogoutRequested = onLogoutRequested,
            departments = departments,
            onCreateDepartment = model::createDepartment,
            onDeleteDepartment = model::delete,
            lendings = lendings,
            onLendingSignUpRequested = onLendingSignUpRequested,
            onCancelLendingRequest = model::cancelLending,
            memoryUploadProgress = memoryUploadProgress,
            onMemoryEditorRequested = onMemoryEditorRequested,
            onMemorySubmitted = model::submitMemory,
            onCreateInsurance = model::createInsurance,
            onFEMECVConnectRequested = model::connectFEMECV,
            onFEMECVDisconnectRequested = model::disconnectFEMECV,
            users = users,
            onPromote = model::promote,
            isSyncing = isSyncing == true,
            onSyncRequested = model::sync,
            inventoryItemTypes = inventoryItemTypes,
            inventoryItemTypesCategories = inventoryItemTypesCategories.orEmpty(),
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

private const val IDX_HOME = 0
private const val IDX_MANAGEMENT = 1
private const val IDX_PROFILE_NOT_ADMIN = 1
private const val IDX_PROFILE_ADMIN = 2

private fun navigationItems(isAdmin: Boolean): List<Pair<ImageVector, @Composable (() -> String)>> {
    return mutableListOf<Pair<ImageVector, @Composable (() -> String)>>().apply {
        add(Icons.Default.Home to { stringResource(Res.string.nav_home) })
        if (isAdmin) {
            add(Icons.Default.SupervisorAccount to { stringResource(Res.string.nav_management) })
        }
        add(Icons.Default.Face to { stringResource(Res.string.nav_profile) })
    }.toList()
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun HomeScreenContent(
    notificationPermissionResult: NotificationPermissionResult?,
    onNotificationPermissionRequest: () -> Unit,
    onNotificationPermissionDenyRequest: () -> Unit,

    profile: ProfileResponse,
    onLogoutRequested: () -> Unit,

    departments: List<Department>?,
    onCreateDepartment: (displayName: String, image: PlatformFile?) -> Job,
    onDeleteDepartment: (Department) -> Job,

    lendings: List<ReferencedLending>?,
    onLendingSignUpRequested: () -> Unit,
    onCancelLendingRequest: (ReferencedLending) -> Job,
    memoryUploadProgress: Pair<Long, Long>?,
    onMemorySubmitted: (ReferencedLending, PlatformFile) -> Job,
    onMemoryEditorRequested: (ReferencedLending) -> Unit,

    onCreateInsurance: CreateInsuranceRequest,
    onFEMECVConnectRequested: (username: String, password: CharArray) -> Deferred<Throwable?>,
    onFEMECVDisconnectRequested: () -> Job,

    users: List<UserData>?,
    onPromote: (UserData) -> Job,

    inventoryItemTypes: List<InventoryItemType>?,
    inventoryItemTypesCategories: Set<String>,
    onCreateInventoryItemType: (displayName: String, description: String, category: String, image: PlatformFile?) -> Job,
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
    val navigationItems = navigationItems(profile.isAdmin)

    val scope = rememberCoroutineScope()
    val pager = rememberPagerState { navigationItems.size }
    val windowSizeClass = calculateWindowSizeClass()
    val snackbarHostState = remember { SnackbarHostState() }

    var displayingShoppingList by remember { mutableStateOf(false) }
    if (displayingShoppingList) {
        ShoppingListDialog(
            shoppingList,
            inventoryItemTypes,
            onContinue = {
                displayingShoppingList = false
                onShoppingListConfirmed()
            }
        ) { displayingShoppingList = false }
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
                    actions = {
                        val showLogoutButton = (pager.currentPage == IDX_PROFILE_ADMIN && profile.isAdmin) || (pager.currentPage == IDX_PROFILE_NOT_ADMIN && profile.isAdmin.not())
                        if (profile.isAdmin) {
                            Badge { Text(stringResource(Res.string.admin)) }
                        }
                        if (showLogoutButton) {
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
                    for ((index, item) in navigationItems.withIndex()) {
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
                visible = pager.currentPage == IDX_HOME && shoppingList.isNotEmpty(),
                enter = slideInHorizontally { it },
                exit = slideOutHorizontally { it },
            ) {
                ExtendedFloatingActionButton(
                    onClick = { displayingShoppingList = true }
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
                        if (profile.isAdmin) {
                            BadgedBox(
                                badge = {
                                    Badge { Text(stringResource(Res.string.admin)) }
                                },
                                content = {
                                    Icon(
                                        Icons.Default.AdminPanelSettings,
                                        stringResource(Res.string.admin)
                                    )
                                }
                            )
                        }
                    }
                ) {
                    for ((index, item) in navigationItems.withIndex()) {
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
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
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

                VerticalPager(
                    state = pager,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = false
                ) { page ->
                    HomeScreenPagerContent(
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
                        onCancelLendingRequest,
                        memoryUploadProgress,
                        onMemorySubmitted,
                        onMemoryEditorRequested,
                        onCreateInsurance,
                        onFEMECVConnectRequested,
                        onFEMECVDisconnectRequested,
                        users,
                        onPromote,
                        inventoryItemTypes,
                        inventoryItemTypesCategories,
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
                    ) { page ->
                        HomeScreenPagerContent(
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
                            onCancelLendingRequest,
                            memoryUploadProgress,
                            onMemorySubmitted,
                            onMemoryEditorRequested,
                            onCreateInsurance,
                            onFEMECVConnectRequested,
                            onFEMECVDisconnectRequested,
                            users,
                            onPromote,
                            inventoryItemTypes,
                            inventoryItemTypesCategories,
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
fun HomeScreenPagerContent(
    page: Int,
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
    onCancelLendingRequest: (ReferencedLending) -> Job,
    memoryUploadProgress: Pair<Long, Long>?,
    onMemorySubmitted: (ReferencedLending, PlatformFile) -> Job,
    onMemoryEditorRequested: (ReferencedLending) -> Unit,

    onCreateInsurance: CreateInsuranceRequest,
    onFEMECVConnectRequested: (username: String, password: CharArray) -> Deferred<Throwable?>,
    onFEMECVDisconnectRequested: () -> Job,

    users: List<UserData>?,
    onPromote: (UserData) -> Job,

    inventoryItemTypes: List<InventoryItemType>?,
    inventoryItemTypesCategories: Set<String>,
    onCreateInventoryItemType: (displayName: String, description: String, category: String, image: PlatformFile?) -> Job,
    onClickInventoryItemType: (InventoryItemType) -> Unit,

    inventoryItems: List<ReferencedInventoryItem>?,

    shoppingList: Map<Uuid, Int>,
    onAddItemToShoppingListRequest: (InventoryItemType) -> Unit,
    onRemoveItemFromShoppingListRequest: (InventoryItemType) -> Unit,

    onManageLendingsRequested: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        when (page) {
            IDX_HOME -> HomeMainPage(
                windowSizeClass,
                snackbarHostState,
                notificationPermissionResult,
                onNotificationPermissionRequest,
                onNotificationPermissionDenyRequest,
                profile,
                inventoryItems,
                lendings,
                onLendingSignUpRequested,
                memoryUploadProgress,
                onMemorySubmitted,
                onMemoryEditorRequested,
                shoppingList,
                onAddItemToShoppingListRequest,
                onRemoveItemFromShoppingListRequest,
                onCancelLendingRequest,
            )

            IDX_MANAGEMENT if profile.isAdmin -> ManagementPage(
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

            IDX_PROFILE_ADMIN if profile.isAdmin -> ProfilePage(
                windowSizeClass,
                profile,
                onCreateInsurance,
                onFEMECVConnectRequested,
                onFEMECVDisconnectRequested,
            )

            IDX_PROFILE_NOT_ADMIN if profile.isAdmin.not() -> ProfilePage(
                windowSizeClass,
                profile,
                onCreateInsurance,
                onFEMECVConnectRequested,
                onFEMECVDisconnectRequested,
            )
        }
    }
}
