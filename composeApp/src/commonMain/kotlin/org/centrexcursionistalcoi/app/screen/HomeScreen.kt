package org.centrexcursionistalcoi.app.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditCalendar
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
import org.centrexcursionistalcoi.app.pages.home.ReservationPage
import org.centrexcursionistalcoi.app.pages.home.SettingsPage
import org.centrexcursionistalcoi.app.platform.ui.PlatformNavigationBar
import org.centrexcursionistalcoi.app.platform.ui.PlatformScaffold
import org.centrexcursionistalcoi.app.route.Home
import org.centrexcursionistalcoi.app.route.Loading
import org.centrexcursionistalcoi.app.viewmodel.HomeViewModel
import org.jetbrains.compose.resources.stringResource

object HomeScreen : Screen<Home, HomeViewModel>(::HomeViewModel) {
    private const val NUM_PAGES = 3

    private const val IDX_HOME = 0
    private const val IDX_RESERVE = 1
    private const val IDX_SETTINGS = 2
    private const val IDX_ADMIN = NUM_PAGES

    @Composable
    override fun Content(viewModel: HomeViewModel) {
        val user by viewModel.userData.collectAsState()
        val itemBookings by viewModel.itemBookings.collectAsState()
        val spaceBookings by viewModel.spaceBookings.collectAsState()

        val usersList by viewModel.usersList.collectAsState()
        val updatingUser by viewModel.updatingUser.collectAsState()
        val sections by viewModel.sections.collectAsState()
        val creatingSection by viewModel.creatingSection.collectAsState()
        val itemTypes by viewModel.itemTypes.collectAsState()
        val creatingType by viewModel.creatingType.collectAsState()
        val items by viewModel.items.collectAsState()
        val creatingItem by viewModel.creatingItem.collectAsState()
        val updatingBooking by viewModel.updatingBooking.collectAsState()
        val allBookings by viewModel.allBookings.collectAsState()
        val spaces by viewModel.spaces.collectAsState()
        val creatingSpace by viewModel.creatingSpace.collectAsState()

        val availableItems by viewModel.availableItems.collectAsState()
        val availableSpaces by viewModel.availableSpaces.collectAsState()

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
                        Icons.Default.CalendarMonth to stringResource(Res.string.nav_home),
                        Icons.Default.EditCalendar to stringResource(Res.string.nav_reserve),
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
                        IDX_HOME -> HomePage(itemBookings, spaceBookings, spaces)
                        IDX_RESERVE -> ReservationPage(
                            itemTypes,
                            availableItems,
                            availableSpaces,
                            viewModel::availability
                        )
                        IDX_SETTINGS -> SettingsPage()
                        IDX_ADMIN -> AdminPage(
                            updatingUser = updatingUser,
                            users = usersList,
                            onUserConfirmationRequested = viewModel::confirm,
                            onUserDeleteRequested = viewModel::delete,
                            sections = sections,
                            isCreatingSection = creatingSection,
                            onSectionOperation = viewModel::onCreateOrUpdate,
                            itemTypes = itemTypes,
                            isCreatingType = creatingType,
                            onTypeOperation = viewModel::createOrUpdate,
                            items = items,
                            isCreatingItem = creatingItem,
                            onItemOperation = viewModel::createOrUpdate,
                            allBookings = allBookings,
                            isUpdatingBooking = updatingBooking,
                            onConfirmBookingRequested = viewModel::confirmBooking,
                            onMarkAsTakenRequested = viewModel::markAsTaken,
                            onMarkAsReturnedRequested = viewModel::markAsReturned,
                            spaces = spaces,
                            isCreatingSpace = creatingSpace,
                            onSpaceOperation = viewModel::createOrUpdate
                        )
                    }
                }
            }
        }
    }
}
