package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.database.DatabaseNowExpression
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp

object Departments : UuidTable("departments") {
    val lastUpdate = timestamp("lastUpdate").defaultExpression(DatabaseNowExpression)

    val displayName = varchar("displayName", 255)
    val image = optReference("image", Files, ReferenceOption.SET_NULL)
}
