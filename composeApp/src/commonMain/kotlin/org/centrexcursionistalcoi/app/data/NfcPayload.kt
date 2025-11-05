package org.centrexcursionistalcoi.app.data

import kotlin.uuid.Uuid

class NfcPayload(
    /**
     * The unique identifier of the NFC tag.
     * May be null or empty if the tag does not provide an ID.
     */
    val id: ByteArray?,
    /**
     * A list of pairs where each pair consists of a MIME type (String) and its corresponding payload (ByteArray).
     */
    val payload: List<Pair<String, ByteArray>>,
) {
    companion object {
        const val MIME_TYPE_GENERIC = "application/vnd.org.centrexcursionistalcoi.nfc"
        const val MIME_TYPE_UUID = "application/vnd.org.centrexcursionistalcoi.uuid"
    }

    /**
     * Retrieves the UUID from the payload if available.
     * @return The [Uuid] object if found, otherwise `null`.
     */
    fun uuid(): Uuid? {
        val item = payload.firstOrNull { it.first == MIME_TYPE_UUID } ?: return null
        return Uuid.fromByteArray(item.second)
    }
}
