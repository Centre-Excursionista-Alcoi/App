package org.centrexcursionistalcoi.app.ui.page.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.DepartmentMemberInfo
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.data.ReferencedPost
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.permission.HelperHolder
import org.centrexcursionistalcoi.app.permission.result.NotificationPermissionResult
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.ui.page.main.home.DepartmentPendingJoinRequest
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
    lendings: List<ReferencedLending>?,
    onLendingClick: (ReferencedLending) -> Unit,
    onOtherUserLendingClick: (ReferencedLending) -> Unit,

    posts: List<ReferencedPost>?,

    departments: List<Department>?,
    onApproveDepartmentJoinRequest: (DepartmentMemberInfo) -> Job,
    onDenyDepartmentJoinRequest: (DepartmentMemberInfo) -> Job,

    users: List<UserData>?,
) {
    val permissionHelper = HelperHolder.getPermissionHelperInstance()
    val isRegisteredForLendings = profile.lendingUser != null
    val isAdmin = profile.isAdmin

    val userLendings = lendings?.filter { it.user.sub == profile.sub }

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

        if (!posts.isNullOrEmpty()) {
            stickyHeader {
                Text(
                    text = stringResource(Res.string.home_posts),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.background(MaterialTheme.colorScheme.background).fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 8.dp),
                )
            }
            items(posts) { post ->
                PostItem(post)
            }
            item(key = "posts_spacer", contentType = "spacer", span = { GridItemSpan(maxLineSpan) }) {
                Spacer(Modifier.height(16.dp))
            }
        }

        // The notification permission is only used for lendings, so don't ask for it if the user is not registered for lendings
        if (isRegisteredForLendings) {
            if (notificationPermissionResult in listOf(NotificationPermissionResult.Denied, NotificationPermissionResult.NotAllowed)) {
                item("notification_permission", contentType = "permission", span = { GridItemSpan(maxLineSpan) }) {
                    CardWithIcon(
                        title = stringResource(Res.string.permission_notification_title),
                        message = stringResource(Res.string.permission_notification_message),
                        icon = Icons.Default.Notifications,
                        contentDescription = stringResource(Res.string.permission_notification_title),
                        modifier = Modifier.padding(bottom = 12.dp),
                    ) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f).padding(end = 4.dp),
                            onClick = onNotificationPermissionDenyRequest,
                        ) {
                            Icon(Icons.Default.Close, stringResource(Res.string.permission_deny))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(Res.string.permission_deny))
                        }
                        if (notificationPermissionResult == NotificationPermissionResult.NotAllowed) {
                            OutlinedButton(
                                modifier = Modifier.weight(1f).padding(start = 4.dp),
                                onClick = { permissionHelper.openSettings() },
                            ) {
                                Icon(Icons.Default.Settings, stringResource(Res.string.permission_settings))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(Res.string.permission_settings))
                            }
                        } else {
                            OutlinedButton(
                                modifier = Modifier.weight(1f).padding(start = 4.dp),
                                onClick = onNotificationPermissionRequest,
                            ) {
                                Icon(Icons.Default.Security, stringResource(Res.string.permission_grant))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(Res.string.permission_grant))
                            }
                        }
                    }
                }
            }

            val activeLendings = userLendings
                ?.filter { it.status() !in listOf(Lending.Status.MEMORY_SUBMITTED, Lending.Status.COMPLETE) }
                ?.sortedByDescending { it.from }
            if (!activeLendings.isNullOrEmpty()) {
                stickyHeader("active_lendings_header") {
                    Text(
                        text = stringResource(Res.string.home_lendings),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.background(MaterialTheme.colorScheme.background).fillMaxWidth().padding(horizontal = 8.dp),
                    )
                }
                items(activeLendings, key = { it.id }, contentType = { "active-lending" }, span = { GridItemSpan(maxLineSpan) }) { lending ->
                    LendingItem(lending) { onLendingClick(lending) }
                }
            }

            val oldLendings = userLendings
                ?.filter { it.status() in listOf(Lending.Status.MEMORY_SUBMITTED, Lending.Status.COMPLETE) }
                ?.sortedByDescending { it.from }
                .orEmpty()
            if (oldLendings.isNotEmpty()) {
                stickyHeader {
                    Text(
                        text = stringResource(Res.string.home_past_lendings),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.background(MaterialTheme.colorScheme.background).fillMaxWidth().padding(horizontal = 8.dp),
                    )
                }
                items(
                    items = oldLendings,
                    key = { it.id },
                    contentType = { "old-lending" },
                ) { lending ->
                    OldLendingItem(lending) { onLendingClick(lending) }
                }
            }
        }

        if (isAdmin) {
            val nonCompletedLendings = lendings?.filter { it.status() !in listOf(Lending.Status.MEMORY_SUBMITTED, Lending.Status.COMPLETE) }.orEmpty()
            if (nonCompletedLendings.isNotEmpty()) {
                stickyHeader {
                    Text(
                        text = stringResource(Res.string.management_other_users_lendings),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.background(MaterialTheme.colorScheme.background).fillMaxWidth().padding(horizontal = 8.dp),
                    )
                }
                items(
                    items = nonCompletedLendings,
                    key = { "_${it.id}" },
                    contentType = { "non-completed-lending" },
                ) { lending ->
                    LendingItem(lending) { onOtherUserLendingClick(lending) }
                }
            }

            val pendingJoinRequests = departments
                ?.map { it.members }
                ?.filter { members -> members.any { !it.confirmed } }
                ?.flatten()
            if (!pendingJoinRequests.isNullOrEmpty()) {
                stickyHeader {
                    Text(
                        text = stringResource(Res.string.management_other_users_join_requests),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.background(MaterialTheme.colorScheme.background).fillMaxWidth().padding(horizontal = 8.dp),
                    )
                }
                items(
                    items = pendingJoinRequests.filterNot { it.confirmed },
                    key = { "join_request_${it.id}" },
                    contentType = { "pending-join-request" },
                ) { request ->
                    val department = departments.find { dept -> dept.members.any { it.id == request.id } } ?: return@items
                    val userData = users?.find { it.sub == request.userSub } ?: return@items

                    DepartmentPendingJoinRequest(
                        userData = userData,
                        department = department,
                        onApprove = { onApproveDepartmentJoinRequest(request) },
                        onDeny = { onDenyDepartmentJoinRequest(request) },
                    )
                }
            }
        }

        item(key = "bottom_spacer", contentType = "spacer") { Spacer(Modifier.height(16.dp)) }
    }
}
