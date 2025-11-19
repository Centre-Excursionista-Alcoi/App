package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable

object Departments : UUIDTable("departments") {
    val displayName = varchar("displayName", 255)
    val image = optReference("image", Files, ReferenceOption.SET_NULL)
}
