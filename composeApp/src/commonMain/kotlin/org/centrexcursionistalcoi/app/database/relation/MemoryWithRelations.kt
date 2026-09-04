package org.centrexcursionistalcoi.app.database.relation

import androidx.room3.Embedded
import androidx.room3.Junction
import androidx.room3.Relation
import org.centrexcursionistalcoi.app.data.ReferencedMemory
import org.centrexcursionistalcoi.app.database.room.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.room.entity.MemberEntity
import org.centrexcursionistalcoi.app.database.room.entity.MemoryEntity
import org.centrexcursionistalcoi.app.database.room.entity.MemoryMemberCrossRef
import org.centrexcursionistalcoi.app.database.room.entity.UserEntity
import org.centrexcursionistalcoi.app.exception.MissingCrossReferenceException

data class MemoryWithRelations(
    @Embedded val memory: MemoryEntity,
    @Relation(parentColumns = ["department"], entityColumns = ["id"])
    val department: DepartmentEntity?,
    @Relation(parentColumns = ["submittedBy"], entityColumns = ["sub"])
    val submittedBy: UserEntity?,
    // Inner-joined via MemoryMemberCrossRef: a memberNumber with no matching local Members row is silently
    // excluded, matching the previous manual `mapNotNull` behavior.
    @Relation(
        entity = MemberEntity::class,
        parentColumns = ["id"],
        entityColumns = ["memberNumber"],
        associateBy = Junction(MemoryMemberCrossRef::class, parentColumns = ["memoryId"], entityColumns = ["memberNumber"]),
    )
    val members: List<MemberEntity>,
)

fun MemoryWithRelations.toReferenced(): ReferencedMemory {
    val submittedByUser = submittedBy?.toUser()
        ?: throw MissingCrossReferenceException("User", memory.submittedBy)
    return ReferencedMemory(
        id = memory.id,
        place = memory.place,
        members = members.map { it.toMember() },
        externalUsers = memory.externalUsers,
        text = memory.text,
        sport = memory.sport,
        department = department?.toDepartment(),
        pdf = memory.pdf,
        attachments = memory.attachments.orEmpty(),
        submittedBy = submittedByUser,
        from = memory.fromDate,
        to = memory.toDate,
        lending = memory.lending,
    )
}
