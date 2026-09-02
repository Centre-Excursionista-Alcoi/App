package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

object MemoriesFiles : Table("memories_files") {
    val memory = reference("memory", Memories, onDelete = ReferenceOption.CASCADE)
    val file = reference("file", Files)

    override val primaryKey: PrimaryKey = PrimaryKey(memory, file, name = "PK_memories_files")
}
