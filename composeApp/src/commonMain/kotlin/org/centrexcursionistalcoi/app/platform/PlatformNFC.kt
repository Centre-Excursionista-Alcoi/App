package org.centrexcursionistalcoi.app.platform

import org.centrexcursionistalcoi.app.data.NfcPayload
import org.koin.core.annotation.Singleton
import kotlin.uuid.Uuid

@Singleton
expect class PlatformNFC : PlatformProvider {
    override val isSupported: Boolean

    /**
     * Reads data from an NFC tag.
     *
     * @return The data read from the NFC tag, or `null` if no data is available. Options: [String], [Uuid].
     */
    suspend fun readNFC(): NfcPayload?

    /**
     * Writes an NDEF message to an NFC tag.
     *
     * @param message The string message to write to the tag.
     */
    suspend fun writeNFC(message: String)
}

val PlatformNFC.isNotSupported: Boolean
    get() = !isSupported
