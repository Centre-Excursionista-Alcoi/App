package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object ItemTypesTable : IntIdTable() {
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)

    val title = varchar("title", 255)
    val description = varchar("description", 1023).nullable()

    val brand = varchar("brand", 255).nullable()
    val model = varchar("model", 255).nullable()

    val section = reference("section", SectionsTable)
}
