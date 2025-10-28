package org.centrexcursionistalcoi.app.permission

import org.centrexcursionistalcoi.app.permission.result.CameraPermissionResult
import org.centrexcursionistalcoi.app.permission.result.LocationPermissionResult
import org.centrexcursionistalcoi.app.permission.result.NotificationPermissionResult
import org.centrexcursionistalcoi.app.permission.result.PermissionResult
import org.centrexcursionistalcoi.app.permission.result.RecordAudioPermissionResult

actual object HelperHolder {
    actual fun getPermissionHelperInstance(): PermissionHelper {
        return object : PermissionHelper {
            override suspend fun <T : PermissionResult> checkIsPermissionGranted(permission: Permission<T>): T {
                @Suppress("UNCHECKED_CAST")
                return when (permission) {
                    is Permission.Location -> {
                        LocationPermissionResult.Granted.Precise
                    }
                    is Permission.Notification -> {
                        NotificationPermissionResult.Granted
                    }
                    is Permission.Camera -> {
                        CameraPermissionResult.Granted
                    }
                    is Permission.RecordAudio -> {
                        RecordAudioPermissionResult.Granted
                    }
                } as T
            }

            override suspend fun <T : PermissionResult> requestForPermission(permission: Permission<T>): T {
                @Suppress("UNCHECKED_CAST")
                return when (permission) {
                    is Permission.Location -> {
                        LocationPermissionResult.Granted.Precise
                    }
                    is Permission.Notification -> {
                        NotificationPermissionResult.Granted
                    }
                    is Permission.Camera -> {
                        CameraPermissionResult.Granted
                    }
                    is Permission.RecordAudio -> {
                        RecordAudioPermissionResult.Granted
                    }
                } as T
            }

            override fun openSettings() {
                // nothing
            }
        }
    }
}
