package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.time
import org.jetbrains.exposed.v1.javatime.timestamp

object Events : UUIDTable("events") {
    val created = timestamp("created").defaultExpression(CurrentTimestamp)
    val updated = timestamp("updated").defaultExpression(CurrentTimestamp)

    val date = date("date")
    val time = time("time").nullable()

    val place = text("place")

    val title = text("title")
    val description = text("description").nullable()

    val maxPeople = integer("maxPeople").nullable()
    val requiresConfirmation = bool("requiresConfirmation").default(false)

    val department = optReference("department", Departments)
    val image = optReference("image", Files)
}
