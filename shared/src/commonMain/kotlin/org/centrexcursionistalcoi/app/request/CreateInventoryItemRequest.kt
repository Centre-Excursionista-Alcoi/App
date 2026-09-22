package org.centrexcursionistalcoi.app.request

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.serializer.Base64Serializer

/**
 * `POST /inventory/items` request body, JSON-only (#659) -- the same shape [UpdateInventoryItemRequest] already
 * uses for a patch, with `type` required since an item can't exist without knowing what type it is.
 */
@Serializable
data class CreateInventoryItemRequest(
    val type: Uuid,
    val variation: String? = null,
    @Serializable(Base64Serializer::class) val nfcId: ByteArray? = null,
    val manufacturerTraceabilityCode: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as CreateInventoryItemRequest

        if (type != other.type) return false
        if (variation != other.variation) return false
        if (!nfcId.contentEquals(other.nfcId)) return false
        if (manufacturerTraceabilityCode != other.manufacturerTraceabilityCode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + (variation?.hashCode() ?: 0)
        result = 31 * result + (nfcId?.contentHashCode() ?: 0)
        result = 31 * result + (manufacturerTraceabilityCode?.hashCode() ?: 0)
        return result
    }
}
