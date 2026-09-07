package org.centrexcursionistalcoi.app.platform

import org.centrexcursionistalcoi.app.data.NfcPayload
import org.koin.core.annotation.Singleton

@Singleton
actual class PlatformNFC : PlatformProvider {
    actual override val isSupported: Boolean = false

    actual suspend fun readNFC(): NfcPayload? {
        throw UnsupportedOperationException("NFC is not supported on JVM platform")
    }

    actual suspend fun writeNFC(message: String) {
        throw UnsupportedOperationException("NFC is not supported on JVM platform")
    }
}
