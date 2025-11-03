package org.centrexcursionistalcoi.app.platform

actual object PlatformLoadLogic {
    actual fun isReady(): Boolean {
        // nothing to check on JVM
        return true
    }

    actual suspend fun load() {
        // nothing to load on JVM
    }
}
