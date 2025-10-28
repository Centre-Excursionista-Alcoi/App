package org.centrexcursionistalcoi.app.permission

import org.centrexcursionistalcoi.app.permission.result.PermissionResult

interface PermissionHelper {
    suspend fun <T: PermissionResult> checkIsPermissionGranted(permission: Permission<T>): T
    suspend fun <T: PermissionResult> requestForPermission(permission: Permission<T>): T
    fun openSettings()
}
