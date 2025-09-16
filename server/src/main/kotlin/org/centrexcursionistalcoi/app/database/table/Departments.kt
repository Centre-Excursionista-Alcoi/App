package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object Departments : IntIdTable("departments") {
    val displayName = varchar("displayName", 255)
}
