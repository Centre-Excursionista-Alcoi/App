package org.centrexcursionistalcoi.app.database.entity

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IdTable

abstract class EntityClass<ID: Any, E: Entity<ID>>(val table: IdTable<ID>) {
    fun wrapRow(row: ResultRow): E {
        table.
    }
}
