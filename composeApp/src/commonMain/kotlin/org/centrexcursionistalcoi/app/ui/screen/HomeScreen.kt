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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
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
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.*
import io.github.vinceglb.filekit.PlatformFile
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.InventoryItem
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.typing.ShoppingList
import org.centrexcursionistalcoi.app.ui.dialog.CreateInsuranceRequest
import org.centrexcursionistalcoi.app.ui.dialog.ShoppingListDialog
import org.centrexcursionistalcoi.app.ui.page.home.HomeMainPage
import org.centrexcursionistalcoi.app.ui.page.home.ManagementPage
import org.centrexcursionistalcoi.app.ui.page.home.ProfilePage
import org.centrexcursionistalcoi.app.ui.platform.calculateWindowSizeClass
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.viewmodel.HomeViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomeScreen(
    onManageLendingsRequested: () -> Unit,
    onShoppingListConfirmed: (ShoppingList) -> Unit,
    onLendingSignUpRequested: () -> Unit,
    model: HomeViewModel = viewModel { HomeViewModel() }
) {
    val profile by model.profile.collectAsState()
    val departments by model.departments.collectAsState()
    val users by model.users.collectAsState()
    val inventoryItemTypes by model.inventoryItemTypes.collectAsState()
    val inventoryItems by model.inventoryItems.collectAsState()
    val lendings by model.lendings.collectAsState()
    val isSyncing by model.isSyncing.collectAsState()
    val shoppingList by model.shoppingList.collectAsState()

    profile?.let {
        HomeScreenContent(
            profile = it,
            departments = departments,
            onCreateDepartment = model::createDepartment,
            onDeleteDepartment = model::delete,
            lendings = lendings,
            onLendingSignUpRequested = onLendingSignUpRequested,
            onCancelLendingRequest = model::cancelLending,
            onMemorySubmitted = model::submitMemory,
            onCreateInsurance = model::createInsurance,
            users = users,
            isSyncing = isSyncing,
            onSyncRequested = model::sync,
            inventoryItemTypes = inventoryItemTypes,
            onCreateInventoryItemType = model::createInventoryItemType,
            onUpdateInventoryItemType = model::updateInventoryItemType,
            onDeleteInventoryItemType = model::delete,
            inventoryItems = inventoryItems,
            onCreateInventoryItem = model::createInventoryItem,
            onDeleteInventoryItem = model::delete,
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
private const val IDX_PROFILE = 2

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
    profile: ProfileResponse,

    departments: List<Department>?,
    onCreateDepartment: (displayName: String, image: PlatformFile?) -> Job,
    onDeleteDepartment: (Department) -> Job,

    lendings: List<Lending>?,
    onLendingSignUpRequested: () -> Unit,
    onCancelLendingRequest: (Lending) -> Job,
    onMemorySubmitted: (Lending, PlatformFile) -> Job,
    onCreateInsurance: CreateInsuranceRequest,

    users: List<UserData>?,

    inventoryItemTypes: List<InventoryItemType>?,
    onCreateInventoryItemType: (displayName: String, description: String, image: PlatformFile?) -> Job,
    onUpdateInventoryItemType: (id: Uuid, displayName: String?, description: String?, image: PlatformFile?) -> Job,
    onDeleteInventoryItemType: (InventoryItemType) -> Job,

    inventoryItems: List<InventoryItem>?,
    onCreateInventoryItem: (variation: String, type: InventoryItemType, amount: Int) -> Job,
    onDeleteInventoryItem: (InventoryItem) -> Job,

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

    Scaffold(
        topBar = {
            if (windowSizeClass.widthSizeClass <= WindowWidthSizeClass.Medium) {
                TopAppBar(
                    title = {},
                    actions = {
                        if (profile.isAdmin) {
                            Badge { Text(stringResource(Res.string.admin)) }
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
                        profile,
                        windowSizeClass,
                        departments,
                        onCreateDepartment,
                        onDeleteDepartment,
                        lendings,
                        onLendingSignUpRequested,
                        onCancelLendingRequest,
                        onMemorySubmitted,
                        onCreateInsurance,
                        users,
                        inventoryItemTypes,
                        onCreateInventoryItemType,
                        onUpdateInventoryItemType,
                        onDeleteInventoryItemType,
                        inventoryItems,
                        onCreateInventoryItem,
                        onDeleteInventoryItem,
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
                            profile,
                            windowSizeClass,
                            departments,
                            onCreateDepartment,
                            onDeleteDepartment,
                            lendings,
                            onLendingSignUpRequested,
                            onCancelLendingRequest,
                            onMemorySubmitted,
                            onCreateInsurance,
                            users,
                            inventoryItemTypes,
                            onCreateInventoryItemType,
                            onUpdateInventoryItemType,
                            onDeleteInventoryItemType,
                            inventoryItems,
                            onCreateInventoryItem,
                            onDeleteInventoryItem,
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
    profile: ProfileResponse,
    windowSizeClass: WindowSizeClass,

    departments: List<Department>?,
    onCreateDepartment: (displayName: String, image: PlatformFile?) -> Job,
    onDeleteDepartment: (Department) -> Job,

    lendings: List<Lending>?,
    onLendingSignUpRequested: () -> Unit,
    onCancelLendingRequest: (Lending) -> Job,
    onMemorySubmitted: (Lending, PlatformFile) -> Job,
    onCreateInsurance: CreateInsuranceRequest,

    users: List<UserData>?,

    inventoryItemTypes: List<InventoryItemType>?,
    onCreateInventoryItemType: (displayName: String, description: String, image: PlatformFile?) -> Job,
    onUpdateInventoryItemType: (id: Uuid, displayName: String?, description: String?, image: PlatformFile?) -> Job,
    onDeleteInventoryItemType: (InventoryItemType) -> Job,

    inventoryItems: List<InventoryItem>?,
    onCreateInventoryItem: (variation: String, type: InventoryItemType, amount: Int) -> Job,
    onDeleteInventoryItem: (InventoryItem) -> Job,

    shoppingList: Map<Uuid, Int>,
    onAddItemToShoppingListRequest: (InventoryItemType) -> Unit,
    onRemoveItemFromShoppingListRequest: (InventoryItemType) -> Unit,

    onManageLendingsRequested: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        when (page) {
            IDX_HOME -> HomeMainPage(
                windowSizeClass,
                profile,
                inventoryItemTypes,
                inventoryItems,
                lendings,
                onLendingSignUpRequested,
                onMemorySubmitted,
                shoppingList,
                onAddItemToShoppingListRequest,
                onRemoveItemFromShoppingListRequest,
                onCancelLendingRequest,
            )

            IDX_MANAGEMENT -> ManagementPage(
                windowSizeClass,
                departments,
                onCreateDepartment,
                onDeleteDepartment,
                users,
                inventoryItemTypes,
                onCreateInventoryItemType,
                onUpdateInventoryItemType,
                onDeleteInventoryItemType,
                inventoryItems,
                onCreateInventoryItem,
                onDeleteInventoryItem,
                onManageLendingsRequested,
            )

            IDX_PROFILE -> ProfilePage(windowSizeClass, profile, onCreateInsurance)

            // 1 -> LendingPage(windowSizeClass, profile, onLendingSignUp, onCreateInsurance)
        }
    }
}
