package org.centrexcursionistalcoi.app.request

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.SpaceKey
import org.centrexcursionistalcoi.app.serializer.Base64Serializer
import kotlin.uuid.Uuid

/**
 * An empty [label] or [nfcId] removes it.
 */
@Serializable
data class UpdateSpaceKeyRequest(
    val label: String? = null,
    @Serializable(Base64Serializer::class) val nfcId: ByteArray? = null,
) : UpdateEntityRequest<Uuid, SpaceKey> {
    override fun isEmpty(): Boolean = label == null && nfcId == null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UpdateSpaceKeyRequest) return false

        return label == other.label && nfcId.contentEquals(other.nfcId)
    }

    override fun hashCode(): Int {
        var result = label?.hashCode() ?: 0
        result = 31 * result + (nfcId?.contentHashCode() ?: 0)
        return result
    }
}
