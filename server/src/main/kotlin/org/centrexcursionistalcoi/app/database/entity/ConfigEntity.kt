package org.centrexcursionistalcoi.app.database.entity

import java.time.Instant
import org.centrexcursionistalcoi.app.database.table.ConfigTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

class ConfigEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, ConfigEntity>(ConfigTable) {
        context(_: JdbcTransaction)
        operator fun <T> set(entry: ConfigEntry<T>, value: T) {
            val str = entry.store(value)
            val entity = findById(entry.key) ?: new(entry.key) {}
            entity.value = str
        }

        context(_: JdbcTransaction)
        operator fun <T> get(entry: ConfigEntry<T>): T? {
            val entity = findById(entry.key) ?: return null
            val value = entity.value
            return entry.retrieve(value)
        }
    }

    var value by ConfigTable.value

    abstract class ConfigEntry<Type>(
        val key: String,
        val retrieve: (String?) -> Type?,
        val store: (Type) -> String,
    )

    object DatabaseVersion : ConfigEntry<Int>(
        key = "database_version",
        retrieve = { it?.toIntOrNull() },
        store = { it.toString() },
    )

    object LastCEASync : ConfigEntry<Instant>(
        key = "last_cea_sync",
        retrieve = { it?.toLongOrNull()?.let { Instant.ofEpochSecond(it) } },
        store = { it.epochSecond.toString() },
    )
}
