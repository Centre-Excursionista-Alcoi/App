package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.database.DatabaseNowExpression
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp

object ReceivedItems : UuidTable() {
    val lending = reference("lending", Lendings, onDelete = ReferenceOption.CASCADE)
    val item = reference("item", InventoryItems, onDelete = ReferenceOption.RESTRICT)
    val notes = text("notes").nullable()

    val receivedBy = reference("receivedBy", UserReferences)
    val receivedAt = timestamp("receivedAt").defaultExpression(DatabaseNowExpression)
}
