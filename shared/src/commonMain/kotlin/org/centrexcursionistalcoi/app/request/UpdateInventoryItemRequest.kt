package org.centrexcursionistalcoi.app.request

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.InventoryItem
import org.centrexcursionistalcoi.app.serializer.Base64Serializer
import kotlin.uuid.Uuid

@Serializable
data class UpdateInventoryItemRequest(
    val variation: String? = null,
    val type: Uuid? = null,
    @Serializable(Base64Serializer::class) val nfcId: ByteArray? = null,
    val manufacturerTraceabilityCode: String? = null,
): UpdateEntityRequest<Uuid, InventoryItem> {
    override fun isEmpty(): Boolean {
        return variation == null && type == null && nfcId == null && manufacturerTraceabilityCode == null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as UpdateInventoryItemRequest

        if (variation != other.variation) return false
        if (type != other.type) return false
        if (!nfcId.contentEquals(other.nfcId)) return false
        if (manufacturerTraceabilityCode != other.manufacturerTraceabilityCode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = variation?.hashCode() ?: 0
        result = 31 * result + (type?.hashCode() ?: 0)
        result = 31 * result + (nfcId?.contentHashCode() ?: 0)
        result = 31 * result + (manufacturerTraceabilityCode?.hashCode() ?: 0)
        return result
    }
}
