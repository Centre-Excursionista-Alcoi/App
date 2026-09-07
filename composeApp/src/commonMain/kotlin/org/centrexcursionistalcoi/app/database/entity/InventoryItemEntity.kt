package org.centrexcursionistalcoi.app.database.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import org.centrexcursionistalcoi.app.data.InventoryItem
import kotlin.uuid.Uuid

@Entity(
    tableName = "InventoryItems",
    foreignKeys = [
        ForeignKey(
            entity = InventoryItemTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["type"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["type"])],
)
data class InventoryItemEntity(
    @PrimaryKey
    val id: Uuid,
    val variation: String?,
    val type: Uuid,
    val nfcId: ByteArray?,
    val manufacturerTraceabilityCode: String?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as InventoryItemEntity

        if (id != other.id) return false
        if (variation != other.variation) return false
        if (type != other.type) return false
        if (!nfcId.contentEquals(other.nfcId)) return false
        if (manufacturerTraceabilityCode != other.manufacturerTraceabilityCode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (variation?.hashCode() ?: 0)
        result = 31 * result + type.hashCode()
        result = 31 * result + (nfcId?.contentHashCode() ?: 0)
        result = 31 * result + (manufacturerTraceabilityCode?.hashCode() ?: 0)
        return result
    }

    fun toInventoryItem() = InventoryItem(
        id = id,
        variation = variation,
        type = type,
        nfcId = nfcId,
        manufacturerTraceabilityCode = manufacturerTraceabilityCode,
    )

    companion object {
        fun InventoryItem.toEntity() = InventoryItemEntity(
            id = id,
            variation = variation,
            type = type,
            nfcId = nfcId,
            manufacturerTraceabilityCode = manufacturerTraceabilityCode,
        )
    }
}
