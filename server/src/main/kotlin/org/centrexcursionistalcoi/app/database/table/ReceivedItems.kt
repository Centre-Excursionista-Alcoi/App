package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp

object ReceivedItems : UUIDTable() {
    val lending = reference("lending", Lendings)
    val item = reference("item", InventoryItems)
    val notes = text("notes").nullable()

    val receivedBy = reference("receivedBy", UserReferences)
    val receivedAt = timestamp("receivedAt").defaultExpression(CurrentTimestamp)
}
