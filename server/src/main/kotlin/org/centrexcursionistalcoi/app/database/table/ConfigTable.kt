package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.v1.core.dao.id.IdTable

object ConfigTable : IdTable<String>("config") {
    override val id = varchar("key", 100).entityId()
    val value = text("value")
}
