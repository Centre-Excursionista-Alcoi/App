package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp

object Posts : UUIDTable("posts") {
    val date = timestamp("date").defaultExpression(CurrentTimestamp)
    val lastUpdate = timestamp("lastUpdate").defaultExpression(CurrentTimestamp)

    val title = varchar("title", 255)
    val content = text("content")
    val department = optReference("department", Departments)
}
