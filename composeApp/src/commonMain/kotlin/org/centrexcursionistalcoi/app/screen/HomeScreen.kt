package org.centrexcursionistalcoi.app.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import ceaapp.composeapp.generated.resources.*
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.composition.AccountStateNavigator
import org.centrexcursionistalcoi.app.pages.home.AdminPage
import org.centrexcursionistalcoi.app.pages.home.HomePage
import org.centrexcursionistalcoi.app.platform.ui.PlatformNavigationBar
import org.centrexcursionistalcoi.app.platform.ui.PlatformScaffold
import org.centrexcursionistalcoi.app.route.Home
import org.centrexcursionistalcoi.app.route.Loading
import org.centrexcursionistalcoi.app.viewmodel.HomeViewModel
import org.jetbrains.compose.resources.stringResource

object HomeScreen : Screen<Home, HomeViewModel>(::HomeViewModel) {
    private const val NUM_PAGES = 2

    private const val IDX_HOME = 0
    private const val IDX_SETTINGS = 1
    private const val IDX_ADMIN = NUM_PAGES

    @Composable
    override fun Content(viewModel: HomeViewModel) {
        val user by viewModel.userData.collectAsState()
        val sections by viewModel.sections.collectAsState()
        val creatingSection by viewModel.creatingSection.collectAsState()
        val itemTypes by viewModel.itemTypes.collectAsState()
        val creatingType by viewModel.creatingType.collectAsState()
        val items by viewModel.items.collectAsState()
        val creatingItem by viewModel.creatingItem.collectAsState()

        val scope = rememberCoroutineScope()
        val pagerState = rememberPagerState { if (user?.isAdmin == true) (NUM_PAGES + 1) else NUM_PAGES }

        LaunchedEffect(Unit) {
            viewModel.load()
        }

        AccountStateNavigator(onLoggedOut = Loading)

        PlatformScaffold(
            title = user?.let { stringResource(Res.string.home_welcome, it.name) } ?: "",
            actions = listOf(
                Triple(
                    Icons.AutoMirrored.Rounded.Logout,
                    stringResource(Res.string.logout)
                ) { viewModel.logout() }
            ),
            navigationBar = {
                PlatformNavigationBar(
                    selection = pagerState.currentPage,
                    onSelectionChanged = { scope.launch { pagerState.animateScrollToPage(it) } },
                    items = listOfNotNull(
                        Icons.Default.Home to stringResource(Res.string.nav_home),
                        Icons.Default.Settings to stringResource(Res.string.nav_settings),
                        if (user?.isAdmin == true) Icons.Default.AdminPanelSettings to stringResource(Res.string.nav_admin) else null
                    )
                )
            }
        ) { paddingValues ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            ) { page ->
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (page) {
                        IDX_HOME -> HomePage(items, itemTypes)
                        IDX_SETTINGS -> {}
                        IDX_ADMIN -> AdminPage(
                            sections = sections,
                            isCreatingSection = creatingSection,
                            onSectionOperation = viewModel::onCreateOrUpdate,
                            itemTypes = itemTypes,
                            isCreatingType = creatingType,
                            onTypeOperation = viewModel::createOrUpdate,
                            items = items,
                            isCreatingItem = creatingItem,
                            onItemOperation = viewModel::createOrUpdate
                        )
                    }
                }
            }
        }
    }
}
