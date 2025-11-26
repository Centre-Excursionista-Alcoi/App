package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp

object InventoryItemTypes : UUIDTable("inventory_item_types") {
    val lastUpdate = timestamp("lastUpdate").defaultExpression(CurrentTimestamp)

    val displayName = text("displayName")
    val description = text("description").nullable()
    val categories = array("categories", TextColumnType()).nullable()
    val department = optReference("department", Departments, ReferenceOption.RESTRICT)
    val image = optReference("image", Files, ReferenceOption.SET_NULL)
}
