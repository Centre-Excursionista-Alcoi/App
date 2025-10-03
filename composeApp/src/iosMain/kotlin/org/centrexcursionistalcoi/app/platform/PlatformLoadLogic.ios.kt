package org.centrexcursionistalcoi.app.platform

actual object PlatformLoadLogic {
    actual fun isReady(): Boolean = true

    actual suspend fun load() {
        // not needed
    }
}
