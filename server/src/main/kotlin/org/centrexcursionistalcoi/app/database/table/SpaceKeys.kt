package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.database.DatabaseNowExpression
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp

/**
 * The keys the club has: each one an exact copy of a [SpaceKeyTypes], identifiable by its id and by its NFC tag, if it
 * has one. Like the items of the inventory.
 */
object SpaceKeys : UuidTable("space_keys") {
    val lastUpdate = timestamp("lastUpdate").defaultExpression(DatabaseNowExpression)

    val type = reference("type", SpaceKeyTypes, onDelete = ReferenceOption.RESTRICT)
    val label = text("label").nullable()
    val nfcId = binary("nfcId").nullable().uniqueIndex()
}
