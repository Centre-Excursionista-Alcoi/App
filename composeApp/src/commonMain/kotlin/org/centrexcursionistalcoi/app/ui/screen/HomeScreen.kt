package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.*
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.InventoryItem
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.ui.dialog.CreateInsuranceRequest
import org.centrexcursionistalcoi.app.ui.page.home.LendingPage
import org.centrexcursionistalcoi.app.ui.page.home.LendingPageOnCreate
import org.centrexcursionistalcoi.app.ui.page.home.ManagementPage
import org.centrexcursionistalcoi.app.ui.platform.calculateWindowSizeClass
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.viewmodel.HomeViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomeScreen(
    onManageLendingsRequested: () -> Unit,
    model: HomeViewModel = viewModel { HomeViewModel() }
) {
    val profile by model.profile.collectAsState()
    val departments by model.departments.collectAsState()
    val users by model.users.collectAsState()
    val inventoryItemTypes by model.inventoryItemTypes.collectAsState()
    val inventoryItems by model.inventoryItems.collectAsState()
    val isSyncing by model.isSyncing.collectAsState()

    profile?.let {
        HomeScreenContent(
            profile = it,
            departments = departments,
            onCreateDepartment = model::createDepartment,
            onDeleteDepartment = model::delete,
            onLendingSignUp = model::signUpForLending,
            onCreateInsurance = model::createInsurance,
            users = users,
            isSyncing = isSyncing,
            onSyncRequested = model::sync,
            inventoryItemTypes = inventoryItemTypes,
            onCreateInventoryItemType = model::createInventoryItemType,
            onDeleteInventoryItemType = model::delete,
            inventoryItems = inventoryItems,
            onCreateInventoryItem = model::createInventoryItem,
            onDeleteInventoryItem = model::delete,
            onManageLendingsRequested = onManageLendingsRequested,
        )
    } ?: LoadingBox()
}

private fun navigationItems(isAdmin: Boolean): List<Pair<ImageVector, @Composable (() -> String)>> {
    return mutableListOf<Pair<ImageVector, @Composable (() -> String)>>().apply {
        add(Icons.Default.Home to { stringResource(Res.string.nav_home) })
        add(Icons.Default.Receipt to { stringResource(Res.string.nav_lending) })
        if (isAdmin) {
            add(Icons.Default.SupervisorAccount to { stringResource(Res.string.nav_management) })
        }
    }.toList()
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun HomeScreenContent(
    profile: ProfileResponse,
    departments: List<Department>?,
    onCreateDepartment: (displayName: String, image: PlatformFile?) -> Job,
    onDeleteDepartment: (Department) -> Job,
    onLendingSignUp: LendingPageOnCreate,
    onCreateInsurance: CreateInsuranceRequest,
    users: List<UserData>?,
    inventoryItemTypes: List<InventoryItemType>?,
    onCreateInventoryItemType: (displayName: String, description: String, image: PlatformFile?) -> Job,
    onDeleteInventoryItemType: (InventoryItemType) -> Job,
    inventoryItems: List<InventoryItem>?,
    onCreateInventoryItem: (variation: String, type: InventoryItemType, amount: Int) -> Job,
    onDeleteInventoryItem: (InventoryItem) -> Job,
    onManageLendingsRequested: () -> Unit,
    isSyncing: Boolean,
    onSyncRequested: () -> Unit
) {
    val navigationItems = navigationItems(profile.isAdmin)

    val scope = rememberCoroutineScope()
    val pager = rememberPagerState { navigationItems.size }
    val windowSizeClass = calculateWindowSizeClass()

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
        }
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
                    NavigationRailItem(
                        selected = false,
                        enabled = !isSyncing,
                        onClick = onSyncRequested,
                        icon = { Icon(Icons.Default.Sync, null) }
                    )
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
                        onLendingSignUp,
                        onCreateInsurance,
                        users,
                        inventoryItemTypes,
                        onCreateInventoryItemType,
                        onDeleteInventoryItemType,
                        inventoryItems,
                        onCreateInventoryItem,
                        onDeleteInventoryItem,
                        onManageLendingsRequested,
                    )
                }
            } else {
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
                        onLendingSignUp,
                        onCreateInsurance,
                        users,
                        inventoryItemTypes,
                        onCreateInventoryItemType,
                        onDeleteInventoryItemType,
                        inventoryItems,
                        onCreateInventoryItem,
                        onDeleteInventoryItem,
                        onManageLendingsRequested,
                    )
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
    onLendingSignUp: LendingPageOnCreate,
    onCreateInsurance: CreateInsuranceRequest,
    users: List<UserData>?,
    inventoryItemTypes: List<InventoryItemType>?,
    onCreateInventoryItemType: (displayName: String, description: String, image: PlatformFile?) -> Job,
    onDeleteInventoryItemType: (InventoryItemType) -> Job,
    inventoryItems: List<InventoryItem>?,
    onCreateInventoryItem: (variation: String, type: InventoryItemType, amount: Int) -> Job,
    onDeleteInventoryItem: (InventoryItem) -> Job,
    onManageLendingsRequested: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        when (page) {
            0 -> Text(
                text = "Welcome back ${profile.username}!",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 24.dp)
            )

            1 -> LendingPage(windowSizeClass, profile, onLendingSignUp, onCreateInsurance)

            2 -> ManagementPage(
                windowSizeClass,
                departments,
                onCreateDepartment,
                onDeleteDepartment,
                users,
                inventoryItemTypes,
                onCreateInventoryItemType,
                onDeleteInventoryItemType,
                inventoryItems,
                onCreateInventoryItem,
                onDeleteInventoryItem,
                onManageLendingsRequested,
            )
        }
    }
}
