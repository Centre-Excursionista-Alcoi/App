package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp

/**
 * Identifies individual items in the inventory.
 */
object InventoryItems : UUIDTable("inventory_items") {
    // ALL COLUMN NAMES MUST MATCH THE FIELD NAMES
    val lastUpdate = timestamp("lastUpdate").defaultExpression(CurrentTimestamp)

    val variation = text("variation").nullable()
    val type = reference("type", InventoryItemTypes)
    val nfcId = binary("nfcId").nullable().uniqueIndex()
}
