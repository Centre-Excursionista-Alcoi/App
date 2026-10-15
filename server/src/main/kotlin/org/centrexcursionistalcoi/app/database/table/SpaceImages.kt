package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

/** The photos of a space. */
object SpaceImages : Table("space_images") {
    val space = reference("space", Spaces, onDelete = ReferenceOption.CASCADE)
    val file = reference("file", Files, onDelete = ReferenceOption.RESTRICT)

    override val primaryKey = PrimaryKey(space, file, name = "PK_SpaceImages_space_file")
}
