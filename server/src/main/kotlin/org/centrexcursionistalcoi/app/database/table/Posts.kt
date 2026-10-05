package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.database.DatabaseNowExpression
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp

object Posts : UuidTable("posts") {
    val date = timestamp("date").defaultExpression(DatabaseNowExpression)
    val lastUpdate = timestamp("lastUpdate").defaultExpression(CurrentTimestamp)

    val title = varchar("title", 255)
    val content = text("content")
    val department = optReference("department", Departments)


    val link = varchar("link", 512).nullable()
}
