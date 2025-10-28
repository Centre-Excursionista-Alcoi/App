package org.centrexcursionistalcoi.app.permission.result

sealed interface LocationPermissionResult: PermissionResult {
    sealed interface Granted: LocationPermissionResult {
        data object Precise: Granted
        data object Approximate: Granted
    }
    data object NotAllowed: LocationPermissionResult
    data object Denied: LocationPermissionResult
}