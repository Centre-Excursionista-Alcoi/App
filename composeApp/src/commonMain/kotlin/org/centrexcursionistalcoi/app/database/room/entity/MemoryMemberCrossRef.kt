package org.centrexcursionistalcoi.app.database.room.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import kotlin.uuid.Uuid

/**
 * Junction row for the many-to-many relation between [MemoryEntity] and [MemberEntity]. No FK to [MemberEntity]: a
 * `memberNumber` here may not have a matching local member row (e.g. an external/removed member), so the relation
 * query is an inner join, silently excluding such rows -- matching the previous manual `mapNotNull` behavior.
 */
@Entity(
    tableName = "MemoryMembers",
    primaryKeys = ["memoryId", "memberNumber"],
    foreignKeys = [
        ForeignKey(
            entity = MemoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["memoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["memberNumber"])],
)
data class MemoryMemberCrossRef(
    val memoryId: Uuid,
    val memberNumber: Long,
)
