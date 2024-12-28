package org.centrexcursionistalcoi.app.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ceaapp.composeapp.generated.resources.*
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.centrexcursionistalcoi.app.component.AppText
import org.centrexcursionistalcoi.app.composition.AccountStateNavigator
import org.centrexcursionistalcoi.app.database.entity.Notification
import org.centrexcursionistalcoi.app.pages.home.AdminPage
import org.centrexcursionistalcoi.app.pages.home.HomePage
import org.centrexcursionistalcoi.app.pages.home.ProfilePage
import org.centrexcursionistalcoi.app.pages.home.ReservationPage
import org.centrexcursionistalcoi.app.platform.ui.PlatformNavigationBar
import org.centrexcursionistalcoi.app.platform.ui.PlatformScaffold
import org.centrexcursionistalcoi.app.platform.ui.getPlatformTextStyles
import org.centrexcursionistalcoi.app.route.Home
import org.centrexcursionistalcoi.app.route.Loading
import org.centrexcursionistalcoi.app.viewmodel.HomeViewModel
import org.jetbrains.compose.resources.stringResource

object HomeScreen : Screen<Home, HomeViewModel>(::HomeViewModel) {
    private const val NUM_PAGES = 3

    private const val IDX_HOME = 0
    private const val IDX_RESERVE = 1
    private const val IDX_PROFILE = 2
    private const val IDX_ADMIN = NUM_PAGES

    @Composable
    override fun Content(viewModel: HomeViewModel) {
        val user by viewModel.userData.collectAsState(null)
        val itemBookings by viewModel.itemBookings.collectAsState(null)
        val spaceBookings by viewModel.spaceBookings.collectAsState(null)

        val notifications by viewModel.notifications.collectAsState(null)
        val notViewedNotifications = notifications.orEmpty().filter { !it.viewed }

        val usersList by viewModel.usersList.collectAsState(null)
        val confirmingUser by viewModel.confirmingUser.collectAsState()
        val updatingUser by viewModel.updatingUser.collectAsState()
        val sections by viewModel.sections.collectAsState(null)
        val creatingSection by viewModel.creatingSection.collectAsState()
        val itemTypes by viewModel.itemTypes.collectAsState(null)
        val items by viewModel.items.collectAsState(null)
        val updatingBooking by viewModel.updatingBooking.collectAsState()
        val allItemBookings by viewModel.allItemBookings.collectAsState(null)
        val allSpaceBookings by viewModel.allSpaceBookings.collectAsState(null)
        val spaces by viewModel.spaces.collectAsState(null)
        val creatingSpace by viewModel.creatingSpace.collectAsState()

        val availableItems by viewModel.availableItems.collectAsState()
        val availableSpaces by viewModel.availableSpaces.collectAsState()

        val scope = rememberCoroutineScope()
        var page by rememberSaveable { mutableStateOf(0) }
        val numPages = rememberSaveable(user) { if (user?.isAdmin == true) (NUM_PAGES + 1) else NUM_PAGES }
        val pagerState = rememberPagerState { numPages }

        LaunchedEffect(Unit) {
            viewModel.load()
        }
        LaunchedEffect(Unit) {
            pagerState.scrollToPage(page)
        }
        LaunchedEffect(route) {
            val showBookingIdString = route.showBookingIdString
            val showUserId = route.showUserId
            if (showUserId != null) {
                viewModel.startUserConfirmation(showUserId)
            } else if (showBookingIdString != null) {
                // TODO: Show booking
            }
        }

        AccountStateNavigator(onLoggedOut = Loading)

        PlatformScaffold(
            navigationBar = {
                PlatformNavigationBar(
                    selection = pagerState.currentPage,
                    onSelectionChanged = {
                        page = it
                        scope.launch { pagerState.animateScrollToPage(it) }
                    },
                    items = listOfNotNull(
                        Icons.Default.CalendarMonth to stringResource(Res.string.nav_home),
                        Icons.Default.EditCalendar to stringResource(Res.string.nav_reserve),
                        Icons.Default.Person to stringResource(Res.string.nav_account),
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
                        IDX_HOME -> HomePage(
                            user,
                            itemBookings,
                            spaceBookings,
                            spaces,
                            notifications,
                            notViewedNotifications,
                            viewModel::markAsViewed
                        )

                        IDX_RESERVE -> ReservationPage(
                            itemTypes,
                            availableItems,
                            availableSpaces,
                            viewModel::availability
                        )

                        IDX_PROFILE -> ProfilePage(
                            user,
                            viewModel::logout
                        )

                        IDX_ADMIN -> AdminPage(
                            updatingUser = updatingUser,
                            users = usersList,
                            onUserConfirmationRequested = viewModel::confirm,
                            onUserDeleteRequested = viewModel::delete,
                            confirmingUser = confirmingUser,
                            onConfirmingUserRequested = viewModel::startUserConfirmation,
                            onConfirmingUserCancelled = viewModel::cancelUserConfirmation,
                            sections = sections,
                            isCreatingSection = creatingSection,
                            onSectionOperation = viewModel::onCreateOrUpdate,
                            itemTypes = itemTypes,
                            items = items,
                            allItemBookings = allItemBookings,
                            allSpaceBookings = allSpaceBookings,
                            isUpdatingBooking = updatingBooking,
                            onCancelBookingRequested = viewModel::cancelBooking,
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

    @Composable
    fun NotificationContent(
        notification: Notification,
        onMarkAsViewed: () -> Unit,
        onClick: () -> Unit
    ) {
        val createdAt = notification.createdAt.toLocalDateTime(TimeZone.currentSystemDefault())

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                AppText(
                    text = notification.title(),
                    modifier = Modifier.padding(top = 8.dp),
                    style = getPlatformTextStyles().label.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontStyle = if (notification.viewed) FontStyle.Italic else FontStyle.Normal
                    )
                )
                AppText(
                    text = notification.message(),
                    style = getPlatformTextStyles().label.copy(
                        fontSize = 12.sp,
                        fontStyle = if (notification.viewed) FontStyle.Italic else FontStyle.Normal
                    )
                )
                AppText(
                    text = "${createdAt.date} ${createdAt.time.hour}:${createdAt.time.minute}",
                    modifier = Modifier.padding(bottom = 8.dp),
                    style = getPlatformTextStyles().label.copy(fontSize = 12.sp)
                )
            }
            if (!notification.viewed) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(Res.string.mark_as_read),
                    modifier = Modifier.padding(4.dp).clickable(onClick = onMarkAsViewed)
                )
            }
        }
    }
}
