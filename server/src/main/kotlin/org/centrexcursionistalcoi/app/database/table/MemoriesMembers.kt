package org.centrexcursionistalcoi.app.database.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

object MemoriesMembers : Table("memories_members") {
    val memory = reference("memory", Memories, onDelete = ReferenceOption.CASCADE)
    val member = reference("member", Members)

    override val primaryKey: PrimaryKey = PrimaryKey(memory, member, name = "PK_MemoriesMembers_memory_member")

    init {
        // There must be only one entry for each memory-member pair
        uniqueIndex("UQ_MemoriesMembers_memory_member", memory, member)
    }
}
