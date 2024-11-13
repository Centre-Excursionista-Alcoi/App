package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.javatime.timestamp

object LendingsTable: IntIdTable("lendings") {
    val item = reference("item", ItemsTable)
    val user = reference("user", UsersTable)

    val createdAt = timestamp("createdAt").defaultExpression(CurrentTimestamp)

    val confirmed = bool("confirmed").default(false)

    val from = datetime("from")
    val to = datetime("to")

    val takenAt = timestamp("takenAt").nullable()
    val returnedAt = timestamp("returnedAt").nullable()
}
