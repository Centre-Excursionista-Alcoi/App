package org.centrexcursionistalcoi.app.platform

actual object PlatformNFC {
    actual val supportsNFC: Boolean = false

    actual suspend fun readNFC(): String? {
        throw UnsupportedOperationException("NFC is not supported on JVM platform")
    }

    actual suspend fun writeNFC(message: String) {
        throw UnsupportedOperationException("NFC is not supported on JVM platform")
    }
}