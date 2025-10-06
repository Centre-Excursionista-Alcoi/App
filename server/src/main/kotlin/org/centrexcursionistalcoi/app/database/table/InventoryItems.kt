package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.v1.core.dao.id.UUIDTable

/**
 * Identifies individual items in the inventory.
 */
object InventoryItems : UUIDTable("inventory_items") {
    val variation = text("variation").nullable()
    val type = reference("type", InventoryItemTypes)
}
