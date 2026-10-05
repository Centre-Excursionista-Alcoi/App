package org.centrexcursionistalcoi.app.database.entity

import kotlinx.datetime.TimeZone
import org.centrexcursionistalcoi.app.data.Memory
import org.centrexcursionistalcoi.app.data.ZonedDateTime
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
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SizedCollection
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import kotlin.time.Instant
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.security.FileReadWriteRules
import org.centrexcursionistalcoi.app.ADMIN_GROUP_NAME

class MemoryEntity(id: EntityID<Uuid>) : UuidEntity(id), LastUpdateEntity, EntityDataConverter<Memory, Uuid>, EntityPatcher<UpdateMemoryRequest> {
    companion object : UuidEntityClass<MemoryEntity>(Memories)

    val createdAt by Memories.createdAt
    override var lastUpdate: Instant by Memories.lastUpdate

    var place by Memories.place
    var externalPeople by Memories.externalPeople
    var text by Memories.text
    var sport by Memories.sport
    var department by DepartmentEntity optionalReferencedOn Memories.department
    var submittedBy by UserReferenceEntity referencedOn Memories.submittedBy

    var fromInstant by Memories.fromInstant
    var fromZone by Memories.fromZone
    var toInstant by Memories.toInstant
    var toZone by Memories.toZone

    /** When the described activity started, as a [ZonedDateTime] combining [fromInstant]/[fromZone]. */
    var from: ZonedDateTime
        get() = ZonedDateTime.fromInstant(fromInstant, TimeZone.of(fromZone))
        set(value) {
            fromInstant = value.toInstant()
            fromZone = value.timeZone.id
        }

    /** When the described activity ended, as a [ZonedDateTime] combining [toInstant]/[toZone]. */
    var to: ZonedDateTime
        get() = ZonedDateTime.fromInstant(toInstant, TimeZone.of(toZone))
        set(value) {
            toInstant = value.toInstant()
            toZone = value.timeZone.id
        }

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
        id = id.value,
        place = place,
        members = members.map { it.memberNumber },
        externalUsers = externalPeople,
        text = text,
        sport = sport,
        department = Memories.department.lookup()?.value,
        attachments = files.map { it.id.value },
        submittedBy = Memories.submittedBy.lookup().value,
        from = from,
        to = to,
        pdf = Memories.pdf.lookup()?.value,
        lending = Memories.lending.lookup()?.value,
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
        request.department?.let { department = DepartmentEntity.findById(it) }
        request.from?.let { from = it }
        request.to?.let { to = it }
        val ownedFileIds = files.map { it.id.value }
        // Same restriction as the attachments given on creation, see MemoriesRoutes
        val attachmentRules = FileReadWriteRules(readUsers = listOf(submittedBy.sub.value), readGroups = listOf(ADMIN_GROUP_NAME))
        request.attachments?.forEach { fileWithContext ->
            val fileEntity = FileEntity.updateOrCreate(fileWithContext, ownedFileIds, attachmentRules) { fileEntity ->
                MemoriesFiles.deleteWhere { (MemoriesFiles.memory eq this@MemoryEntity.id) and (MemoriesFiles.file eq fileEntity.id) }
            }
            if (fileEntity != null) {
                // A new attachment was created (fileWithContext had bytes) -- link it to this memory.
                MemoriesFiles.insert {
                    it[memory] = this@MemoryEntity.id
                    it[file] = fileEntity.id
                }
            }
        }
    }

    override fun delete() {
        val files = files.toList() + listOfNotNull(pdf)
        super.delete() // memories_files cascades
        FileEntity.deleteOwnedFiles(files)
    }
}
