package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.database.DatabaseNowExpression
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp

/**
 * A type of key of a space. A lending takes up to [maxQuantity] of them.
 */
object SpaceKeys : UuidTable("space_keys") {
    val lastUpdate = timestamp("lastUpdate").defaultExpression(DatabaseNowExpression)

    val space = reference("space", Spaces, onDelete = ReferenceOption.CASCADE)
    val name = text("name")
    val maxQuantity = integer("maxQuantity").default(1)
    val nfcId = binary("nfcId").nullable().uniqueIndex()
}
