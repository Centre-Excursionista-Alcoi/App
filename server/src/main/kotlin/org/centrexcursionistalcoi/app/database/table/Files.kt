package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.v1.core.dao.id.UUIDTable

object Files : UUIDTable("files") {
    val data = binary("data")
    val type = varchar("type", 255)
    val name = varchar("name", 255)
}
