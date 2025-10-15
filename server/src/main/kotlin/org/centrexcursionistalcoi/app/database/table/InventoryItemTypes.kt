package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable

object InventoryItemTypes : UUIDTable("inventory_item_types") {
    val displayName = text("displayName")
    val description = text("description").nullable()
    val image = optReference("image", Files, ReferenceOption.SET_NULL)
}
