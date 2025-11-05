package org.centrexcursionistalcoi.app.platform

import org.centrexcursionistalcoi.app.data.NfcPayload

actual object PlatformNFC : PlatformProvider {
    actual override val isSupported: Boolean = false

    actual suspend fun readNFC(): NfcPayload? {
        throw NotImplementedError()
    }

    actual suspend fun writeNFC(message: String) {
        throw NotImplementedError()
    }
}
