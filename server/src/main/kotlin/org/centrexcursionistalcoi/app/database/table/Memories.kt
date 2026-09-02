package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.database.DatabaseNowExpression
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.javatime.timestamp

object Memories : UUIDTable("memories") {
    val createdAt = timestamp("createdAt").defaultExpression(DatabaseNowExpression)
    val lastUpdate = timestamp("lastUpdate").defaultExpression(DatabaseNowExpression)

    val place = text("place").nullable()
    val externalPeople = text("externalPeople").nullable()
    val text = text("text")
    val sport = enumerationByName("sport", 50, Sports::class).nullable()
    val department = optReference("department", Departments, onDelete = ReferenceOption.SET_NULL)

    // Who submitted the memory. Always set: memories can only be created by a logged-in user.
    val submittedBy = reference("submittedBy", UserReferences, onDelete = ReferenceOption.CASCADE)

    val lending = optReference("lending", Lendings, onDelete = ReferenceOption.RESTRICT)
    val pdf = optReference("pdf", Files, onDelete = ReferenceOption.SET_NULL)

    init {
        // There must be only one memory per lending, if the lending is set
        uniqueIndex("memories_lending_unique", lending)
    }
}
