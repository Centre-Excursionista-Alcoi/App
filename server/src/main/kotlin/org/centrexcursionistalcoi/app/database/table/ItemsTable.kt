package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.data.enumeration.ItemHealth
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object ItemsTable : IntIdTable("items") {
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)

    val notes = varchar("notes", 1023).nullable()

    val health = enumeration("health", ItemHealth::class)

    val type = reference("type", ItemTypesTable)
}
