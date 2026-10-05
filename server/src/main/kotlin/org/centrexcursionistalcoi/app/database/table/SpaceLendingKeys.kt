package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

/**
 * The keys taken by a space lending, and who handed them out and took them back.
 */
object SpaceLendingKeys : Table("space_lending_keys") {
    val lending = reference("lending", SpaceLendings, onDelete = ReferenceOption.CASCADE)
    val key = reference("key", SpaceKeys, onDelete = ReferenceOption.CASCADE)
    val quantity = integer("quantity")

    val givenBy = optReference("givenBy", UserReferences, onDelete = ReferenceOption.SET_NULL)
    val givenAt = timestamp("givenAt").nullable()

    val returnedTo = optReference("returnedTo", UserReferences, onDelete = ReferenceOption.SET_NULL)
    val returnedAt = timestamp("returnedAt").nullable()

    override val primaryKey = PrimaryKey(lending, key, name = "PK_SpaceLendingKeys")
}
