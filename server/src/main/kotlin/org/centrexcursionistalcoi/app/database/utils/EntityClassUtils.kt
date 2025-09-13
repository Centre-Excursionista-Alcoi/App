package org.centrexcursionistalcoi.app.database.utils

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.centrexcursionistalcoi.app.database.entity.Entity
import org.centrexcursionistalcoi.app.database.entity.EntityClass
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll

suspend fun <ID: Any, E: Entity<ID>> EntityClass<ID, E>.getAll(): List<E> {
    return table.selectAll().map { wrapRow(it) }.toList()
}

suspend fun <ID: Any, E: Entity<ID>, T: IdTable<ID>> EntityClass<ID, E>.insert(
    body: T.(InsertStatement<Number>) -> Unit
): E {
    @Suppress("UNCHECKED_CAST")
    val statement = (table as T).insert(body)
    val id = statement[table.id]
    val query = table.selectAll().where { table.id eq id }.limit(1)
    return wrapRow(query.first())
}
