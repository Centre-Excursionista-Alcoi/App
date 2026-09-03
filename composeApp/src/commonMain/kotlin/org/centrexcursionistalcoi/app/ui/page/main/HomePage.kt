package org.centrexcursionistalcoi.app.ui.page.main

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.permission_deny
import cea_app.composeapp.generated.resources.permission_grant
import cea_app.composeapp.generated.resources.permission_notification_message
import cea_app.composeapp.generated.resources.permission_notification_title
import cea_app.composeapp.generated.resources.permission_settings
import cea_app.composeapp.generated.resources.posts
import cea_app.composeapp.generated.resources.upcoming_events
import cea_app.composeapp.generated.resources.welcome
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.data.ReferencedEvent
import org.centrexcursionistalcoi.app.data.ReferencedPost
import org.centrexcursionistalcoi.app.permission.HelperHolder
import org.centrexcursionistalcoi.app.permission.result.NotificationPermissionResult
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Close
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Notifications
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Security
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Settings
import org.centrexcursionistalcoi.app.ui.page.main.home.EventItem
import org.centrexcursionistalcoi.app.ui.page.main.home.PostItem
import org.centrexcursionistalcoi.app.ui.reusable.AdaptiveVerticalGrid
import org.centrexcursionistalcoi.app.ui.reusable.CardWithIcon
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.viewmodel.HomePageModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

@Composable
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
fun HomePage(
    model: HomePageModel = koinViewModel()
) {
    val windowSizeClass = calculateWindowSizeClass()

    val profile = model.profile.collectAsState()
    val posts by model.posts.collectAsState()
    val events by model.events.collectAsState()

    LifecycleResumeEffect(model) {
        model.refreshPermissions()
        onPauseOrDispose { /* nothing */ }
    }

    val profileValue = profile.value
    if (profileValue == null) {
        LoadingBox()
        return
    }

    HomePage(
        windowSizeClass = windowSizeClass,

        notificationPermissionResult = model.notificationPermissionResult.collectAsState().value,
        onNotificationPermissionRequest = { model.requestNotificationsPermission() },
        onNotificationPermissionDenyRequest = { model.denyNotificationsPermission() },

        profile = profileValue,

        posts = posts,

        events = events,
        onConfirmAssistanceRequest = { event -> model.confirmEventAssistance(event) },
        onRejectAssistanceRequest = { event -> model.rejectEventAssistance(event) },
    )
}

@Composable
fun HomePage(
    windowSizeClass: WindowSizeClass,

    notificationPermissionResult: NotificationPermissionResult?,
    onNotificationPermissionRequest: () -> Unit,
    onNotificationPermissionDenyRequest: () -> Unit,

    profile: ProfileResponse,

    posts: List<ReferencedPost>?,

    events: List<ReferencedEvent>?,
    onConfirmAssistanceRequest: (ReferencedEvent) -> Job,
    onRejectAssistanceRequest: (ReferencedEvent) -> Job,
) {
    val permissionHelper = HelperHolder.getPermissionHelperInstance()
    val isRegisteredForLendings = remember(profile) { profile.lendingUser != null }

    val now = Clock.System.now()
    val futureEvents = remember(events) {
        events?.filter { event ->
            event.end?.let { it >= now } ?: (event.start <= now)
        }
    }

    AdaptiveVerticalGrid(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
    ) {
        item(key = "top_spacer", contentType = "spacer", span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(16.dp)) }

        if (windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium) {
            item("welcome_message", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(Res.string.welcome, profile.fullName),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp, top = 12.dp)
                )
            }
        }

        // The notification permission is only used for lendings, so don't ask for it if the user is not registered for lendings
        if (isRegisteredForLendings && notificationPermissionResult in listOf(NotificationPermissionResult.Denied, NotificationPermissionResult.NotAllowed)) {
            item("notification_permission", contentType = "permission", span = { GridItemSpan(maxLineSpan) }) {
                CardWithIcon(
                    title = stringResource(Res.string.permission_notification_title),
                    message = stringResource(Res.string.permission_notification_message),
                    icon = MaterialSymbols.Notifications,
                    contentDescription = stringResource(Res.string.permission_notification_title),
                    modifier = Modifier.padding(bottom = 12.dp),
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f).padding(end = 4.dp),
                        onClick = onNotificationPermissionDenyRequest,
                    ) {
                        Icon(MaterialSymbols.Close, stringResource(Res.string.permission_deny))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(Res.string.permission_deny))
                    }
                    if (notificationPermissionResult == NotificationPermissionResult.NotAllowed) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f).padding(start = 4.dp),
                            onClick = { permissionHelper.openSettings() },
                        ) {
                            Icon(MaterialSymbols.Settings, stringResource(Res.string.permission_settings))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(Res.string.permission_settings))
                        }
                    } else {
                        OutlinedButton(
                            modifier = Modifier.weight(1f).padding(start = 4.dp),
                            onClick = onNotificationPermissionRequest,
                        ) {
                            Icon(MaterialSymbols.Security, stringResource(Res.string.permission_grant))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(Res.string.permission_grant))
                        }
                    }
                }
            }
        }

        if (!futureEvents.isNullOrEmpty()) {
            item("events_title", contentType = "title", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(Res.string.upcoming_events),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
            }
            items(futureEvents) { event ->
                EventItem(
                    profile,
                    event,
                    { onConfirmAssistanceRequest(event) },
                    { onRejectAssistanceRequest(event) },
                )
            }
            // Fill the current line
            item(key = "events_filler", contentType = "filler", span = { GridItemSpan(maxCurrentLineSpan) }) {
                Spacer(Modifier.height(16.dp))
            }
            if (!posts.isNullOrEmpty()) {
                // Add padding between events and posts
                item(
                    key = "events_posts_padding",
                    contentType = "filler",
                    span = { GridItemSpan(maxCurrentLineSpan) },
                ) { Spacer(Modifier.height(12.dp)) }
            }
        }

        if (!posts.isNullOrEmpty()) {
            item("posts_title", contentType = "title", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(Res.string.posts),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            items(posts) { post ->
                PostItem(post)
            }
            // Fill the current line
            item(key = "posts_filler", contentType = "filler", span = { GridItemSpan(maxCurrentLineSpan) }) {
                Spacer(Modifier.height(16.dp))
            }
        }

        item(key = "bottom_spacer", contentType = "spacer") { Spacer(Modifier.height(16.dp)) }
    }
}
