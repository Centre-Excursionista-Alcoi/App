package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.data.Memory
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.base.EntityPatcher
import org.centrexcursionistalcoi.app.database.entity.base.LastUpdateEntity
import org.centrexcursionistalcoi.app.database.table.Members
import org.centrexcursionistalcoi.app.database.table.Memories
import org.centrexcursionistalcoi.app.database.table.MemoriesFiles
import org.centrexcursionistalcoi.app.database.table.MemoriesMembers
import org.centrexcursionistalcoi.app.now
import org.centrexcursionistalcoi.app.request.UpdateMemoryRequest
import org.centrexcursionistalcoi.app.routes.helper.notifyUpdateForEntity
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SizedCollection
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import java.time.Instant
import java.util.UUID
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

class MemoryEntity(id: EntityID<UUID>) : UUIDEntity(id), LastUpdateEntity, EntityDataConverter<Memory, Uuid>, EntityPatcher<UpdateMemoryRequest> {
    companion object : UUIDEntityClass<MemoryEntity>(Memories)

    val createdAt by Memories.createdAt
    override var lastUpdate: Instant by Memories.lastUpdate

    var place by Memories.place
    var externalPeople by Memories.externalPeople
    var text by Memories.text
    var sport by Memories.sport
    var department by DepartmentEntity optionalReferencedOn Memories.department
    var submittedBy by UserReferenceEntity referencedOn Memories.submittedBy
    var lending by LendingEntity optionalReferencedOn Memories.lending
    var pdf by FileEntity optionalReferencedOn Memories.pdf

    // Members and memories are many-to-many. There's a table MemoriesMembers that links them.
    var members by MemberEntity via MemoriesMembers

    // A memory can have multiple files attached, and files aren't shared between memories. There's a table
    // MemoriesFiles that links them.
    val files by FileEntity via MemoriesFiles

    override suspend fun updated() {
        notifyUpdateForEntity(Companion, id)
        Database { lastUpdate = now() }
    }

    context(_: JdbcTransaction)
    override fun toData(): Memory = Memory(
        id = id.value.toKotlinUuid(),
        place = place,
        members = members.map { it.memberNumber },
        externalUsers = externalPeople,
        text = text,
        sport = sport,
        department = department?.id?.value?.toKotlinUuid(),
        files = files.map { it.id.value.toKotlinUuid() },
        submittedBy = submittedBy.id.value,
        pdf = pdf?.id?.value?.toKotlinUuid(),
        lending = lending?.id?.value?.toKotlinUuid(),
    )

    context(_: JdbcTransaction)
    override fun patch(request: UpdateMemoryRequest) {
        request.place?.let { place = it }
        request.members?.let { memberNumbers ->
            members = SizedCollection(MemberEntity.find { Members.id inList memberNumbers }.toList())
        }
        request.externalUsers?.let { externalPeople = it }
        request.text?.let { text = it }
        request.sport?.let { sport = it }
        request.department?.let { department = DepartmentEntity.findById(it.toJavaUuid()) }
        request.files?.forEach { fileWithContext ->
            FileEntity.updateOrCreate(fileWithContext) { fileEntity ->
                MemoriesFiles.deleteWhere { (MemoriesFiles.memory eq this@MemoryEntity.id) and (MemoriesFiles.file eq fileEntity.id) }
            }
        }
    }
}
