package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.database.data.ItemHealth
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object ItemsTable : IntIdTable("items") {
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)

    val health = enumeration("health", ItemHealth::class)
    val amount = integer("amount")

    val type = reference("type", ItemTypesTable)
}
