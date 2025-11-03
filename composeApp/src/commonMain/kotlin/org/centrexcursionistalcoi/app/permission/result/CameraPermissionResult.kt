package org.centrexcursionistalcoi.app.permission.result

sealed interface CameraPermissionResult: PermissionResult {
    data object Granted: CameraPermissionResult
    data object NotAllowed: CameraPermissionResult
    data object Denied: CameraPermissionResult
}