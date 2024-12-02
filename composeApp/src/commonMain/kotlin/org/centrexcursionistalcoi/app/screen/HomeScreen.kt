package org.centrexcursionistalcoi.app.screen

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ceaapp.composeapp.generated.resources.*
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.centrexcursionistalcoi.app.component.AppText
import org.centrexcursionistalcoi.app.composition.AccountStateNavigator
import org.centrexcursionistalcoi.app.data.enumeration.NotificationType
import org.centrexcursionistalcoi.app.database.entity.Notification
import org.centrexcursionistalcoi.app.pages.home.AdminPage
import org.centrexcursionistalcoi.app.pages.home.HomePage
import org.centrexcursionistalcoi.app.pages.home.ReservationPage
import org.centrexcursionistalcoi.app.pages.home.SettingsPage
import org.centrexcursionistalcoi.app.platform.ui.Action
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
    private const val IDX_SETTINGS = 2
    private const val IDX_ADMIN = NUM_PAGES

    @Composable
    override fun Content(viewModel: HomeViewModel) {
        val user by viewModel.userData.collectAsState(null)
        val itemBookings by viewModel.itemBookings.collectAsState(null)
        val spaceBookings by viewModel.spaceBookings.collectAsState(null)

        val notifications by viewModel.notifications.collectAsState(null)
        val notViewedNotifications = notifications.orEmpty().filter { !it.viewed }
        val updatingNotification by viewModel.updatingNotification.collectAsState()

        val usersList by viewModel.usersList.collectAsState(null)
        val updatingUser by viewModel.updatingUser.collectAsState()
        val sections by viewModel.sections.collectAsState(null)
        val creatingSection by viewModel.creatingSection.collectAsState()
        val itemTypes by viewModel.itemTypes.collectAsState(null)
        val creatingType by viewModel.creatingType.collectAsState()
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
        val pagerState = rememberPagerState { if (user?.isAdmin == true) (NUM_PAGES + 1) else NUM_PAGES }

        LaunchedEffect(Unit) {
            viewModel.load()
        }
        LaunchedEffect(Unit) {
            pagerState.scrollToPage(page)
        }

        AccountStateNavigator(onLoggedOut = Loading)

        PlatformScaffold(
            title = user?.let { stringResource(Res.string.home_welcome, it.name) } ?: "",
            actions = listOf(
                Action(
                    Icons.Default.Notifications,
                    stringResource(Res.string.home_notifications),
                    isPrimary = false,
                    badge = notViewedNotifications.size.takeIf { it > 0 }?.toString(),
                    popupContent = {
                        for (notification in notifications.orEmpty()) {
                            NotificationContent(notification) {
                                viewModel.markAsViewed(notification)
                            }
                        }
                        if (notifications.isNullOrEmpty()) {
                            AppText(
                                text = stringResource(Res.string.home_notifications_empty),
                                textAlign = TextAlign.Center
                            )
                        }
                    },
                    onClick = {}
                ),
                Action(
                    Icons.AutoMirrored.Rounded.Logout,
                    stringResource(Res.string.logout)
                ) { viewModel.logout() }
            ),
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
        onMarkAsViewed: () -> Unit
    ) {
        val createdAt = notification.createdAt.toLocalDateTime(TimeZone.currentSystemDefault())

        Column(
            modifier = Modifier.clickable(enabled = !notification.viewed) { onMarkAsViewed() }
        ) {
            AppText(
                text = when (notification.type) {
                    NotificationType.BookingConfirmed -> stringResource(Res.string.notification_booking_confirmed_title)
                    NotificationType.BookingCancelled -> stringResource(Res.string.notification_booking_cancelled_title)

                    else -> "Unsupported notification type: ${notification.type}"
                },
                modifier = Modifier.padding(top = 8.dp),
                style = getPlatformTextStyles().label.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontStyle = if (notification.viewed) FontStyle.Italic else FontStyle.Normal
                )
            )
            AppText(
                text = when (notification.type) {
                    NotificationType.BookingConfirmed -> stringResource(Res.string.notification_booking_confirmed_message)
                    NotificationType.BookingCancelled -> stringResource(Res.string.notification_booking_cancelled_message)

                    else -> "Unsupported notification type: ${notification.type}"
                },
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
    }
}
