package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.data.SpaceLendingFileKind
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

object SpaceLendingFiles : Table("space_lending_files") {
    val lending = reference("lending", SpaceLendings, onDelete = ReferenceOption.CASCADE)
    val file = reference("file", Files)
    val kind = enumerationByName<SpaceLendingFileKind>("kind", 20)

    override val primaryKey = PrimaryKey(lending, file, name = "PK_SpaceLendingFiles")
}
