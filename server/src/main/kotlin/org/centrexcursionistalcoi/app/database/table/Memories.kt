package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.data.ZonedDateTime
import org.centrexcursionistalcoi.app.database.DatabaseNowExpression
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.datetime.timestamp

object Memories : UuidTable("memories") {
    val createdAt = timestamp("createdAt").defaultExpression(DatabaseNowExpression)
    val lastUpdate = timestamp("lastUpdate").defaultExpression(DatabaseNowExpression)

    val place = text("place").nullable()
    val externalPeople = text("externalPeople").nullable()
    val text = text("text")
    val sport = enumerationByName("sport", 50, Sports::class).nullable()
    val department = optReference("department", Departments, onDelete = ReferenceOption.SET_NULL)

    // Who submitted the memory. Always set: memories can only be created by a logged-in user.
    val submittedBy = reference("submittedBy", UserReferences, onDelete = ReferenceOption.CASCADE)

    // When the described activity took place. For lending memories this is filled in automatically from the
    // lending's from/to; for standalone memories the client must provide it. Stored as a plain instant plus the IANA
    // zone id it was recorded in, and exposed as a combined "from"/"to" `ZonedDateTime` (see `MemoryEntity.from`).
    val fromInstant = timestamp("fromInstant")
    val fromZone = varchar("fromZone", 64)
    val toInstant = timestamp("toInstant")
    val toZone = varchar("toZone", 64)

    val lending = optReference("lending", Lendings, onDelete = ReferenceOption.RESTRICT)
    val pdf = optReference("pdf", Files, onDelete = ReferenceOption.SET_NULL)

    init {
        // There must be only one memory per lending, if the lending is set
        uniqueIndex("memories_lending_unique", lending)

        check("memories_from_is_before_to") { fromInstant lessEq toInstant }
    }
}
