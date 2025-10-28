package org.centrexcursionistalcoi.app.permission

import org.centrexcursionistalcoi.app.permission.result.CameraPermissionResult
import org.centrexcursionistalcoi.app.permission.result.LocationPermissionResult
import org.centrexcursionistalcoi.app.permission.result.NotificationPermissionResult
import org.centrexcursionistalcoi.app.permission.result.PermissionResult
import org.centrexcursionistalcoi.app.permission.result.RecordAudioPermissionResult

sealed class Permission<T: PermissionResult> {
    data object Location : Permission<LocationPermissionResult>()
    data object Notification : Permission<NotificationPermissionResult>()
    data object Camera : Permission<CameraPermissionResult>()
    data object RecordAudio : Permission<RecordAudioPermissionResult>()
}
