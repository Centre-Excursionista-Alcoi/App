package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.database.DatabaseNowExpression
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.javatime.timestamp

/**
 * Identifies individual items in the inventory.
 */
object InventoryItems : UUIDTable("inventory_items") {
    // ALL COLUMN NAMES MUST MATCH THE FIELD NAMES
    val lastUpdate = timestamp("lastUpdate").defaultExpression(DatabaseNowExpression)

    val variation = text("variation").nullable()
    val type = reference("type", InventoryItemTypes)
    val nfcId = binary("nfcId").nullable().uniqueIndex()
    val manufacturerTraceabilityCode = text("manufacturerTraceabilityCode").nullable().uniqueIndex()
}
