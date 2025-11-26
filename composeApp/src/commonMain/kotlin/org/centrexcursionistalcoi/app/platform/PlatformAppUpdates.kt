package org.centrexcursionistalcoi.app.platform

import kotlinx.coroutines.flow.Flow

expect object PlatformAppUpdates {
    val updateAvailable: Flow<Boolean>
    val updateProgress: Flow<Float?>
    val restartRequired: Flow<Boolean>

    fun dismissUpdateAvailable()
    fun startUpdate()
    fun onRestartRequested()
}
