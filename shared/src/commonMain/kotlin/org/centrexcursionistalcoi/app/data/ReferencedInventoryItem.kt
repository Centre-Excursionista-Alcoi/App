package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.serializer.Base64Serializer
import kotlin.uuid.Uuid

@Serializable
data class ReferencedInventoryItem(
    override val id: Uuid,
    val variation: String?,
    val type: ReferencedInventoryItemType,
    @Serializable(Base64Serializer::class) val nfcId: ByteArray?,
    val manufacturerTraceabilityCode: String?,
    override val referencedEntity: InventoryItem
): ReferencedEntity<Uuid, InventoryItem>() {
    companion object {
        fun InventoryItem.referenced(type: ReferencedInventoryItemType) = ReferencedInventoryItem(
            id = this.id,
            variation = this.variation,
            type = type,
            nfcId = this.nfcId,
            manufacturerTraceabilityCode = manufacturerTraceabilityCode,
            referencedEntity = this,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ReferencedInventoryItem

        if (id != other.id) return false
        if (variation != other.variation) return false
        if (type != other.type) return false
        if (!nfcId.contentEquals(other.nfcId)) return false
        if (manufacturerTraceabilityCode != other.manufacturerTraceabilityCode) return false
        if (referencedEntity != other.referencedEntity) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (variation?.hashCode() ?: 0)
        result = 31 * result + type.hashCode()
        result = 31 * result + (nfcId?.contentHashCode() ?: 0)
        result = 31 * result + (manufacturerTraceabilityCode?.hashCode() ?: 0)
        result = 31 * result + referencedEntity.hashCode()
        return result
    }
}
