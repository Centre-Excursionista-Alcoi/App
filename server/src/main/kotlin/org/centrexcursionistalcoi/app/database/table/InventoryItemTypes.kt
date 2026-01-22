package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.database.DatabaseNowExpression
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.javatime.timestamp

object InventoryItemTypes : UUIDTable("inventory_item_types") {
    val lastUpdate = timestamp("lastUpdate").defaultExpression(DatabaseNowExpression)

    val displayName = text("displayName")
    val description = text("description").nullable()
    val categories = array("categories", TextColumnType()).nullable()
    val weight = double("weight").nullable()
    val department = optReference("department", Departments, ReferenceOption.RESTRICT)
    val image = optReference("image", Files, ReferenceOption.SET_NULL)
}
