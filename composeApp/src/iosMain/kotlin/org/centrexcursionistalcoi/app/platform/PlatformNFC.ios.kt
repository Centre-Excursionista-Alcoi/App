package org.centrexcursionistalcoi.app.platform

actual object PlatformNFC : PlatformProvider {
    actual override val isSupported: Boolean = false

    actual suspend fun readNFC(): String? {
        throw NotImplementedError()
    }

    actual suspend fun writeNFC(message: String) {
        throw NotImplementedError()
    }
}
