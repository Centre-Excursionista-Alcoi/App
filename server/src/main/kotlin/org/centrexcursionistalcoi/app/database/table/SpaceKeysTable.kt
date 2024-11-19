package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object SpaceKeysTable : IntIdTable() {
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)

    val name = varchar("name", 255)
    val description = text("description").nullable()

    val space = reference("space_id", SpacesTable)
}
