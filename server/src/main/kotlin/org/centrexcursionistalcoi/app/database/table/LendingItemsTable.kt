package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object LendingItemsTable : IntIdTable() {
    val item = reference("item", ItemsTable, onDelete = ReferenceOption.CASCADE)
    val lending = reference("lending", LendingsTable, onDelete = ReferenceOption.CASCADE)
}
