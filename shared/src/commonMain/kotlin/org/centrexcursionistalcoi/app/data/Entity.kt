package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable

@Serializable
sealed interface Entity<IdType: Any> {
    val id: IdType

    /**
     * Convert the entity to a map of key-value pairs, where keys are column names and values are the corresponding values.
     *
     * This can be used by clients to request the creation of new entities by providing a map of values.
     */
    fun toMap(): Map<String, Any?>
}
