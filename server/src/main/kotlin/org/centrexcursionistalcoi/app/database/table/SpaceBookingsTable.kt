package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp

object SpaceBookingsTable : IntIdTable() {
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)

    val from = date("from")
    val to = date("to")

    val confirmed = bool("confirmed").default(false)

    // If the space requires a key, this field will be filled once the user takes it
    val key = reference("key_id", SpaceKeysTable).nullable()

    // When the user takes and returns the key, those fields will be filled
    val takenAt = timestamp("taken_at").nullable()
    val returnedAt = timestamp("returned_at").nullable()

    // Payment information
    val paid = bool("paid").default(false)
    val paymentReference = varchar("payment_reference", 255).nullable()
    val paymentDocument = binary("payment_document").nullable()

    val space = reference("space_id", SpacesTable)
    val user = reference("user_id", UsersTable)
}
