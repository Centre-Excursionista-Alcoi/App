package backend.data.database

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InventoryEntry(
    val id: Long,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("inventory_item") val inventoryItemId: Long,
    val exit: Boolean
)
