package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

/**
 * How many keys of each type a space lending asks for. The exact keys are chosen when they are handed over, see
 * [SpaceLendingKeys].
 */
object SpaceLendingKeyRequests : Table("space_lending_key_requests") {
    val lending = reference("lending", SpaceLendings, onDelete = ReferenceOption.CASCADE)
    val keyType = reference("keyType", SpaceKeyTypes, onDelete = ReferenceOption.RESTRICT)
    val quantity = integer("quantity")

    override val primaryKey = PrimaryKey(lending, keyType, name = "PK_SpaceLendingKeyRequests")
}
