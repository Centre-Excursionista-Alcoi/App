package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.database.DatabaseNowExpression
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp

object Events : UuidTable("events") {
    val created = timestamp("created").defaultExpression(DatabaseNowExpression)
    val lastUpdate = timestamp("lastUpdate").defaultExpression(DatabaseNowExpression)

    val start = timestamp("start")
    val end = timestamp("end").nullable()

    val place = text("place")

    val title = text("title")
    val description = text("description").nullable()

    val maxPeople = long("maxPeople").nullable()
    val requiresConfirmation = bool("requiresConfirmation").default(false)
    val requiresInsurance = bool("requiresInsurance").default(false)

    val department = optReference("department", Departments)
    val image = optReference("image", Files)
}
