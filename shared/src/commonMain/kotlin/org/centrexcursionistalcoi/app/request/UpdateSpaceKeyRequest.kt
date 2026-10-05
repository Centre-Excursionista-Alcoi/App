package org.centrexcursionistalcoi.app.request

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.SpaceKey
import org.centrexcursionistalcoi.app.serializer.Base64Serializer
import kotlin.uuid.Uuid

/**
 * An empty [nfcId] removes the NFC tag of the key.
 */
@Serializable
data class UpdateSpaceKeyRequest(
    val name: String? = null,
    val maxQuantity: Int? = null,
    @Serializable(Base64Serializer::class) val nfcId: ByteArray? = null,
) : UpdateEntityRequest<Uuid, SpaceKey> {
    override fun isEmpty(): Boolean = name == null && maxQuantity == null && nfcId == null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UpdateSpaceKeyRequest) return false

        return name == other.name && maxQuantity == other.maxQuantity && nfcId.contentEquals(other.nfcId)
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + (maxQuantity ?: 0)
        result = 31 * result + (nfcId?.contentHashCode() ?: 0)
        return result
    }
}
