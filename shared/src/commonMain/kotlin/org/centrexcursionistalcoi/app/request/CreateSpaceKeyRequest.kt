package org.centrexcursionistalcoi.app.request

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.serializer.Base64Serializer
import kotlin.uuid.Uuid

@Serializable
data class CreateSpaceKeyRequest(
    /** The [org.centrexcursionistalcoi.app.data.SpaceKeyType] of the key. */
    val type: Uuid,
    val label: String? = null,
    @Serializable(Base64Serializer::class) val nfcId: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CreateSpaceKeyRequest) return false

        return type == other.type && label == other.label && nfcId.contentEquals(other.nfcId)
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + (label?.hashCode() ?: 0)
        result = 31 * result + (nfcId?.contentHashCode() ?: 0)
        return result
    }
}
