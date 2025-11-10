package org.centrexcursionistalcoi.app.data

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.serializer.Base64Serializer

@Serializable
data class InventoryItem(
    override val id: Uuid,
    val variation: String?,
    val type: Uuid,
    @Serializable(Base64Serializer::class) val nfcId: ByteArray?,
): Entity<Uuid> {
    override fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "variation" to variation,
        "type" to type,
        "nfcId" to nfcId,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as InventoryItem

        if (id != other.id) return false
        if (variation != other.variation) return false
        if (type != other.type) return false
        if (!nfcId.contentEquals(other.nfcId)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (variation?.hashCode() ?: 0)
        result = 31 * result + type.hashCode()
        result = 31 * result + (nfcId?.contentHashCode() ?: 0)
        return result
    }
}
