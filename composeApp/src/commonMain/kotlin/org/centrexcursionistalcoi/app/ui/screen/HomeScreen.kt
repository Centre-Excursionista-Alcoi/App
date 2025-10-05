package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SupervisorAccount
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.data.Department
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
fun HomeScreen(model: HomeViewModel = viewModel { HomeViewModel() }) {
    val profile by model.profile.collectAsState()
    val departments by model.departments.collectAsState()

    profile?.let {
        HomeScreenContent(it, departments, model::createDepartment, model::delete, model::signUpForLending, model::createInsurance)
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
    onCreateDepartment: (displayName: String) -> Job,
    onDeleteDepartment: (Department) -> Job,
    onLendingSignUp: LendingPageOnCreate,
    onCreateInsurance: CreateInsuranceRequest,
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
    onCreateDepartment: (displayName: String) -> Job,
    onDeleteDepartment: (Department) -> Job,
    onLendingSignUp: LendingPageOnCreate,
    onCreateInsurance: CreateInsuranceRequest,
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

            2 -> ManagementPage(windowSizeClass, departments, onCreateDepartment, onDeleteDepartment)
        }
    }
}
