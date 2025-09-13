package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.javatime.timestamp

object Posts : UUIDTable("posts") {
    val date = timestamp("date")
    val title = varchar("title", 255)
    val content = text("content")
    val department = reference("department_id", Departments)
}
