package org.centrexcursionistalcoi.app.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

actual object PlatformAppUpdates {
    actual val updateAvailable: Flow<Boolean>
        get() = flowOf(false)
    actual val updateProgress: Flow<Float?>
        get() = flowOf(null)
    actual val restartRequired: Flow<Boolean>
        get() = flowOf(false)

    actual fun onRestartRequested() {
        // nothing
    }

    actual fun dismissUpdateAvailable() {
        // nothing
    }

    actual fun startUpdate() {
        // nothing
    }
}
