package org.centrexcursionistalcoi.app.database.utils

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.centrexcursionistalcoi.app.utils.setPrivateDelegatedValue
import org.centrexcursionistalcoi.app.utils.setPrivateValue
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.dao.entityCache
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import kotlin.jvm.javaClass
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.r2dbc.Query

@Suppress("UNCHECKED_CAST")
private val <ID: Any, E: Entity<ID>> EntityClass<ID, E>.kklass: KClass<E> get() = javaClass.enclosingClass.kotlin as KClass<E>

@Suppress("UNCHECKED_CAST")
private val <ID: Any, E: Entity<ID>> EntityClass<ID, E>.klass: Class<E> get() = javaClass.enclosingClass as Class<E>

@Suppress("UNCHECKED_CAST")
private val <ID: Any, E: Entity<ID>> EntityClass<ID, E>.entityPrimaryCtor: KFunction<E> get() =
    klass.kotlin.primaryConstructor as KFunction<E>

private fun <ID: Any, E: Entity<ID>> EntityClass<ID, E>.entityCtor(entityID: EntityID<ID>): E {
    return entityPrimaryCtor.call(entityID)
}

context(transaction: R2dbcTransaction)
private fun <ID: Any, E: Entity<ID>, EC: EntityClass<ID, E>> EC.wrapR2dbc(
    id: EntityID<ID>,
    entityKClass: KClass<E>
): E {
    return transaction.entityCache.find(this, id) ?: entityCtor(id).also { new ->
        setPrivateDelegatedValue(new, entityKClass, "klass", this)
        setPrivateDelegatedValue(new, entityKClass, "db", transaction.db)
        transaction.entityCache.store(this, new)
    }
}

context(transaction: R2dbcTransaction)
private fun <ID: Any, E: Entity<ID>> EntityClass<ID, E>.wrapRowR2dbc(id: EntityID<ID>, row: ResultRow): E {
    val entity = wrapR2dbc(id, kklass)
    if (entity._readValues == null) entity._readValues = row
    return entity
}

context(transaction: R2dbcTransaction, entityClass: EntityClass<ID, E>)
private suspend fun <ID: Any, E: Entity<ID>> Query.wrap(): List<E> {
    return map { row ->
        entityClass.wrapRowR2dbc(row[entityClass.table.id], row)
    }.toList()
}


context(transaction: R2dbcTransaction)
suspend fun <ID: Any, E: Entity<ID>> EntityClass<ID, E>.getAll(): List<E> {
    return table.selectAll().wrap()
}

context(transaction: R2dbcTransaction)
suspend fun <ID: Any, E: Entity<ID>> EntityClass<ID, E>.get(predicate: () -> Op<Boolean>): List<E> {
    return table.selectAll().where(predicate).wrap()
}

context(transaction: R2dbcTransaction)
suspend fun <ID: Any, E: Entity<ID>, T: IdTable<ID>> EntityClass<ID, E>.insert(
    body: T.(InsertStatement<Number>) -> Unit
): E {
    @Suppress("UNCHECKED_CAST")
    val statement = (table as T).insert(body)
    val id = statement[table.id]
    val query = table.selectAll().where { table.id eq id }.limit(1)
    val row = query.first()
    return wrapRowR2dbc(row[table.id], row)
}
