package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.database.DatabaseNowExpression
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.timestamp

object Lendings : UuidTable("Lendings") {
    val userSub = reference("userSub", UserReferences, onDelete = ReferenceOption.CASCADE)
    val timestamp = timestamp("timestamp").defaultExpression(DatabaseNowExpression)
    val lastUpdate = timestamp("lastUpdate").defaultExpression(DatabaseNowExpression)
    val from = date("from")
    val to = date("to")

    val confirmed = bool("confirmed").default(false)

    val taken = bool("taken").default(false)
    val givenBy = reference("givenBy", UserReferences).nullable()
    val givenAt = timestamp("givenAt").nullable()

    val returned = bool("returned").default(false)

    val memorySubmitted = bool("memorySubmitted").default(false)
    val memorySubmittedAt = timestamp("memorySubmittedAt").nullable()
    val memoryReviewed = bool("memoryReviewed").default(false)

    val notes = text("notes").nullable()

    init {
        check("from_is_before_to") { from lessEq to }
    }
}
