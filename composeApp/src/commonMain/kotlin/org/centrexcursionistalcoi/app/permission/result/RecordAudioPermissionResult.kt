package org.centrexcursionistalcoi.app.permission.result

sealed interface RecordAudioPermissionResult: PermissionResult {
    data object Granted: RecordAudioPermissionResult
    data object NotAllowed: RecordAudioPermissionResult
    data object Denied: RecordAudioPermissionResult
}