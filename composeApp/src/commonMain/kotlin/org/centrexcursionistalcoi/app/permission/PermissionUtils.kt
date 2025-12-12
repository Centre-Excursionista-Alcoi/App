package org.centrexcursionistalcoi.app.permission

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.permission.result.CameraPermissionResult

/**
 * Launches the given [onPermissionGranted] only if the camera permission is granted.
 * Otherwise, requests it to the user.
 * Once the permission is granted, the function will be called.
 */
fun CoroutineScope.launchWithCameraPermission(onPermissionGranted: () -> Unit): Job {
    val permissionHelper = HelperHolder.getPermissionHelperInstance()

    return launch(defaultAsyncDispatcher) {
        when (permissionHelper.checkIsPermissionGranted(Permission.Camera)) {
            CameraPermissionResult.Denied -> {
                val result = permissionHelper.requestForPermission(Permission.Camera)
                if (result == CameraPermissionResult.Granted) onPermissionGranted()
            }
            CameraPermissionResult.NotAllowed -> permissionHelper.openSettings()
            CameraPermissionResult.Granted -> onPermissionGranted()
        }
    }
}
