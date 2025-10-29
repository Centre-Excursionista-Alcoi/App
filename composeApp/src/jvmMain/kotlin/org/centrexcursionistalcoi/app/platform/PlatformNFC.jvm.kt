package org.centrexcursionistalcoi.app.platform

actual object PlatformNFC : PlatformProvider {
    actual override val isSupported: Boolean = false

    actual suspend fun readNFC(): String? {
        throw UnsupportedOperationException("NFC is not supported on JVM platform")
    }

    actual suspend fun writeNFC(message: String) {
        throw UnsupportedOperationException("NFC is not supported on JVM platform")
    }
}
