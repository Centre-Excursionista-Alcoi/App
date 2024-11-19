package org.centrexcursionistalcoi.app.database.common

import org.centrexcursionistalcoi.app.database.table.UsersTable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp

abstract class BookingTable(name: String = "") : IntIdTable(name) {
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)

    val from = date("from")
    val to = date("to")

    val confirmed = bool("confirmed").default(false)

    val takenAt = timestamp("taken_at").nullable()
    val returnedAt = timestamp("returned_at").nullable()

    val user = reference("user_id", UsersTable)
}
