package org.centrexcursionistalcoi.app.permission

import org.centrexcursionistalcoi.app.permission.result.CameraPermissionResult
import org.centrexcursionistalcoi.app.permission.result.LocationPermissionResult
import org.centrexcursionistalcoi.app.permission.result.NotificationPermissionResult
import org.centrexcursionistalcoi.app.permission.result.PermissionResult
import org.centrexcursionistalcoi.app.permission.result.RecordAudioPermissionResult
import tech.kotlinlang.permission.Permission as LibPermission
import tech.kotlinlang.permission.PermissionHelper as LibPermissionHelper
import tech.kotlinlang.permission.result.CameraPermissionResult as LibCameraPermissionResult
import tech.kotlinlang.permission.result.LocationPermissionResult as LibLocationPermissionResult
import tech.kotlinlang.permission.result.NotificationPermissionResult as LibNotificationPermissionResult
import tech.kotlinlang.permission.result.RecordAudioPermissionResult as LibRecordAudioPermissionResult

fun <LT, AT: PermissionResult> Permission<AT>.asLibPermission(): LibPermission<LT> {
    @Suppress("UNCHECKED_CAST")
    return when (this) {
        is Permission.Camera -> LibPermission.Camera as LibPermission<LT>
        is Permission.Location -> LibPermission.Location as LibPermission<LT>
        is Permission.Notification -> LibPermission.Notification as LibPermission<LT>
        is Permission.RecordAudio -> LibPermission.RecordAudio as LibPermission<LT>
    }
}

fun Any?.asAppPermissionResult(): PermissionResult {
    @Suppress("UNCHECKED_CAST")
    return when (this) {
        is LibCameraPermissionResult -> when (this) {
            is LibCameraPermissionResult.Granted -> CameraPermissionResult.Granted
            is LibCameraPermissionResult.NotAllowed -> CameraPermissionResult.NotAllowed
            is LibCameraPermissionResult.Denied -> CameraPermissionResult.Denied
        }

        is LibLocationPermissionResult -> when (this) {
            is LibLocationPermissionResult.Granted.Precise -> LocationPermissionResult.Granted.Precise
            is LibLocationPermissionResult.Granted.Approximate -> LocationPermissionResult.Granted.Approximate
            is LibLocationPermissionResult.NotAllowed -> LocationPermissionResult.NotAllowed
            is LibLocationPermissionResult.Denied -> LocationPermissionResult.Denied
        }

        is LibNotificationPermissionResult -> when (this) {
            is LibNotificationPermissionResult.Granted -> NotificationPermissionResult.Granted
            is LibNotificationPermissionResult.NotAllowed -> NotificationPermissionResult.NotAllowed
            is LibNotificationPermissionResult.Denied -> NotificationPermissionResult.Denied
        }

        is LibRecordAudioPermissionResult -> when (this) {
            is LibRecordAudioPermissionResult.Granted -> RecordAudioPermissionResult.Granted
            is LibRecordAudioPermissionResult.NotAllowed -> RecordAudioPermissionResult.NotAllowed
            is LibRecordAudioPermissionResult.Denied -> RecordAudioPermissionResult.Denied
        }

        else -> throw IllegalArgumentException("Unknown permission type: $this")
    }
}

fun LibPermissionHelper.asAppPermissionHelper(): PermissionHelper = object : PermissionHelper {
    override suspend fun <T: PermissionResult> checkIsPermissionGranted(permission: Permission<T>): T {
        @Suppress("UNCHECKED_CAST")
        return this@asAppPermissionHelper.checkIsPermissionGranted(
            permission.asLibPermission<Any, T>()
        ).asAppPermissionResult() as T
    }

    override suspend fun <T: PermissionResult> requestForPermission(permission: Permission<T>): T {
        @Suppress("UNCHECKED_CAST")
        return this@asAppPermissionHelper.requestForPermission(
            permission.asLibPermission<Any, T>()
        ).asAppPermissionResult() as T
    }

    override fun openSettings() {
        this@asAppPermissionHelper.openSettings()
    }
}
