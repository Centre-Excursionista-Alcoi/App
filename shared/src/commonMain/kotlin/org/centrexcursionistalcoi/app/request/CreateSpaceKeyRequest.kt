package org.centrexcursionistalcoi.app.request

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.serializer.Base64Serializer
import kotlin.uuid.Uuid

@Serializable
data class CreateSpaceKeyRequest(
    val space: Uuid,
    val name: String,
    val maxQuantity: Int = 1,
    @Serializable(Base64Serializer::class) val nfcId: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CreateSpaceKeyRequest) return false

        return maxQuantity == other.maxQuantity &&
            space == other.space &&
            name == other.name &&
            nfcId.contentEquals(other.nfcId)
    }

    override fun hashCode(): Int {
        var result = maxQuantity
        result = 31 * result + space.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (nfcId?.contentHashCode() ?: 0)
        return result
    }
}
