package org.centrexcursionistalcoi.app.request

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.InventoryItem
import org.centrexcursionistalcoi.app.serializer.Base64Serializer

@Serializable
data class UpdateInventoryItemRequest(
    val variation: String? = null,
    val type: Uuid? = null,
    @Serializable(Base64Serializer::class) val nfcId: ByteArray? = null,
): UpdateEntityRequest<Uuid, InventoryItem> {
    override fun isEmpty(): Boolean {
        return variation == null && type == null && nfcId == null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as UpdateInventoryItemRequest

        if (variation != other.variation) return false
        if (type != other.type) return false
        if (!nfcId.contentEquals(other.nfcId)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = variation?.hashCode() ?: 0
        result = 31 * result + (type?.hashCode() ?: 0)
        result = 31 * result + (nfcId?.contentHashCode() ?: 0)
        return result
    }
}
