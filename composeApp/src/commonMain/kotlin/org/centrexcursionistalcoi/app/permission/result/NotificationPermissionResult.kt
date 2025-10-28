package org.centrexcursionistalcoi.app.permission.result

sealed interface NotificationPermissionResult: PermissionResult {
    data object Granted: NotificationPermissionResult
    data object NotAllowed: NotificationPermissionResult
    data object Denied: NotificationPermissionResult
}