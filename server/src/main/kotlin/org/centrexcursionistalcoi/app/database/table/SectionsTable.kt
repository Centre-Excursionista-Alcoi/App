package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object SectionsTable : IntIdTable("Sections") {
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)

    val displayName = varchar("display_name", 255)
}
