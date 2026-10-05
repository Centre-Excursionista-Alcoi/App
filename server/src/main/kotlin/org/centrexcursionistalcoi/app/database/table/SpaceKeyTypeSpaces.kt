package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

/**
 * The spaces a [SpaceKeyTypes] gives access to, and how many keys of the type a lending of the space can take. A type
 * can be for several spaces.
 */
object SpaceKeyTypeSpaces : Table("space_key_type_spaces") {
    val keyType = reference("keyType", SpaceKeyTypes, onDelete = ReferenceOption.CASCADE)
    val space = reference("space", Spaces, onDelete = ReferenceOption.CASCADE)
    val maxPerLending = integer("maxPerLending").default(1)

    override val primaryKey = PrimaryKey(keyType, space, name = "PK_SpaceKeyTypeSpaces")
}
