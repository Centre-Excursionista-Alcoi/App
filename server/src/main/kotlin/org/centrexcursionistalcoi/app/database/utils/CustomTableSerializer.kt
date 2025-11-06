package org.centrexcursionistalcoi.app.database.utils

import kotlinx.serialization.SerializationStrategy
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

interface CustomTableSerializer<ID: Any, E: Entity<ID>> {
    /**
     * Returns a list of the serializers for the extra columns to be included in the serialized output.
     * @return A map of extra column names to their corresponding serialization strategies.
     */
    fun columnSerializers(): Map<String, SerializationStrategy<*>>

    /**
     * Returns a map of extra columns to be included in the serialized output for the given [entity].
     *
     * The keys of the map should correspond to the names returned by [columnNames].
     *
     * The values can be of any type that is serializable.
     *
     * @param entity The entity for which to retrieve extra columns.
     * @return A map of extra column names to their corresponding values.
     */
    context(_: JdbcTransaction)
    fun extraColumns(entity: E): Map<String, Any?>
}
