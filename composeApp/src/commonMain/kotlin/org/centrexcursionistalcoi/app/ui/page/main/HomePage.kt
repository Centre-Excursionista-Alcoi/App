package org.centrexcursionistalcoi.app.ui.page.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.data.ReferencedEvent
import org.centrexcursionistalcoi.app.data.ReferencedPost
import org.centrexcursionistalcoi.app.permission.HelperHolder
import org.centrexcursionistalcoi.app.permission.result.NotificationPermissionResult
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.*
import org.centrexcursionistalcoi.app.ui.page.main.home.EventItem
import org.centrexcursionistalcoi.app.ui.page.main.home.PostItem
import org.centrexcursionistalcoi.app.ui.reusable.AdaptiveVerticalGrid
import org.centrexcursionistalcoi.app.ui.reusable.CardWithIcon
import org.jetbrains.compose.resources.stringResource

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

    AdaptiveVerticalGrid(
        windowSizeClass,
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

        if (!events.isNullOrEmpty()) {
            item("events_title", contentType = "title", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(Res.string.upcoming_events),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
            }
            items(events) { event ->
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
        }

        if (!posts.isNullOrEmpty()) {
            item("posts_title", contentType = "title", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(Res.string.posts),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
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
