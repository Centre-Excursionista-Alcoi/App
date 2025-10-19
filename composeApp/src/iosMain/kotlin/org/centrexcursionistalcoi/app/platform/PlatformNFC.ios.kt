package org.centrexcursionistalcoi.app.platform

actual object PlatformNFC {
    actual val supportsNFC: Boolean = false

    actual suspend fun readNFC(): String? {
        throw NotImplementedError()
    }

    actual suspend fun writeNFC(message: String) {
        throw NotImplementedError()
    }
}
