package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.data.Entity
import org.centrexcursionistalcoi.app.security.UserSession
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

/**
 * An entity of the database that is also a data class shared with the app: what the server answers is this.
 *
 * References are filled with the id the row has (`Table.column.lookup()` inside the entity), not by reading the
 * property: that would load the referenced entity, a query for each entity of a list.
 */
interface EntityDataConverter<DataEntity : Entity<IdType>, IdType: Any> {
    context(_: JdbcTransaction)
    fun toData(): DataEntity

    /**
     * Like [toData], for the user of [session] (`null` if none): some data depends on who asks.
     */
    context(_: JdbcTransaction)
    fun toData(session: UserSession?): DataEntity = toData()
}
