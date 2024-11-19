package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object SpaceIncidencesTable: IntIdTable() {
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)

    val title = varchar("name", 255)
    val body = text("body").nullable()

    val booking = reference("booking_id", SpaceBookingsTable)
}
