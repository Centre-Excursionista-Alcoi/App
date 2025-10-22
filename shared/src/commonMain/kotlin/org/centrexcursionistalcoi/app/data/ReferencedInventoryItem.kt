package org.centrexcursionistalcoi.app.data

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
data class ReferencedInventoryItem(
    override val id: Uuid,
    val variation: String?,
    val type: InventoryItemType,
    override val referencedEntity: InventoryItem
): ReferencedEntity<Uuid, InventoryItem>() {
    companion object {
        fun InventoryItem.referenced(type: InventoryItemType) = ReferencedInventoryItem(
            id = this.id,
            variation = this.variation,
            type = type,
            referencedEntity = this,
        )
    }
}
