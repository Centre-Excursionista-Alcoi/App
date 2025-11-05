package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.v1.core.dao.id.UUIDTable

/**
 * Identifies individual items in the inventory.
 */
object InventoryItems : UUIDTable("inventory_items") {
    // ALL COLUMN NAMES MUST MATCH THE FIELD NAMES

    val variation = text("variation").nullable()
    val type = reference("type", InventoryItemTypes)
    val nfcId = binary("nfcId").nullable().uniqueIndex()
}
