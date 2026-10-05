package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.data.Category
import org.centrexcursionistalcoi.app.data.SpaceLending
import org.centrexcursionistalcoi.app.data.SpaceLendingFileKind
import org.centrexcursionistalcoi.app.data.SpaceLendingKey
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.base.LastUpdateEntity
import org.centrexcursionistalcoi.app.database.table.SpaceLendingFiles
import org.centrexcursionistalcoi.app.database.table.SpaceLendingKeys
import org.centrexcursionistalcoi.app.database.table.SpaceLendings
import org.centrexcursionistalcoi.app.now
import org.centrexcursionistalcoi.app.routes.helper.notifyUpdateForEntity
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.selectAll
import kotlin.time.Instant
import kotlin.uuid.Uuid

class SpaceLendingEntity(id: EntityID<Uuid>) : UuidEntity(id), LastUpdateEntity, EntityDataConverter<SpaceLending, Uuid> {
    override var lastUpdate: Instant by SpaceLendings.lastUpdate

    val timestamp by SpaceLendings.timestamp

    var userSub by UserReferenceEntity optionalReferencedOn SpaceLendings.userSub
    var space by SpaceEntity referencedOn SpaceLendings.space

    var checkIn by SpaceLendings.checkIn
    var checkOut by SpaceLendings.checkOut

    var attendees: Map<Category, Int> by SpaceLendings.attendees
    var acceptedConditionsAt by SpaceLendings.acceptedConditionsAt
    var cancelled by SpaceLendings.cancelled
    var notes by SpaceLendings.notes

    var pickedUpAt by SpaceLendings.pickedUpAt
    var pickedUpBy by UserReferenceEntity optionalReferencedOn SpaceLendings.pickedUpBy
    var returnedAt by SpaceLendings.returnedAt
    var returnedBy by UserReferenceEntity optionalReferencedOn SpaceLendings.returnedBy

    var totalPrice by SpaceLendings.totalPrice
    var paymentStatus by SpaceLendings.paymentStatus

    var reportNotes by SpaceLendings.reportNotes
    var reportIssues by SpaceLendings.reportIssues
    var reportSubmittedAt by SpaceLendings.reportSubmittedAt

    override suspend fun updated() {
        notifyUpdateForEntity(Companion, id)
        Database { lastUpdate = now() }
    }

    context(_: JdbcTransaction)
    fun keys(): List<SpaceLendingKey> = SpaceLendingKeys.selectAll()
        .where { SpaceLendingKeys.lending eq id }
        .map {
            SpaceLendingKey(
                key = it[SpaceLendingKeys.key].value,
                quantity = it[SpaceLendingKeys.quantity],
                givenBy = it[SpaceLendingKeys.givenBy]?.value,
                givenAt = it[SpaceLendingKeys.givenAt],
                returnedTo = it[SpaceLendingKeys.returnedTo]?.value,
                returnedAt = it[SpaceLendingKeys.returnedAt],
            )
        }

    context(_: JdbcTransaction)
    fun fileIds(kind: SpaceLendingFileKind): List<Uuid> = SpaceLendingFiles.selectAll()
        .where { (SpaceLendingFiles.lending eq id) and (SpaceLendingFiles.kind eq kind) }
        .map { it[SpaceLendingFiles.file].value }

    context(_: JdbcTransaction)
    fun files(kind: SpaceLendingFileKind): List<FileEntity> = fileIds(kind).mapNotNull { FileEntity.findById(it) }

    context(_: JdbcTransaction)
    override fun toData(): SpaceLending = SpaceLending(
        id = id.value,
        lastUpdate = lastUpdate,
        timestamp = timestamp,
        space = space.id.value,
        userSub = userSub?.id?.value,
        checkIn = checkIn,
        checkOut = checkOut,
        attendees = attendees,
        acceptedConditionsAt = acceptedConditionsAt,
        cancelled = cancelled,
        notes = notes,
        pickedUpAt = pickedUpAt,
        pickedUpBy = pickedUpBy?.id?.value,
        returnedAt = returnedAt,
        returnedBy = returnedBy?.id?.value,
        keys = keys(),
        totalPrice = totalPrice,
        paymentStatus = paymentStatus,
        reportNotes = reportNotes,
        reportIssues = reportIssues,
        reportSubmittedAt = reportSubmittedAt,
        reportNotesFiles = fileIds(SpaceLendingFileKind.REPORT_NOTES),
        reportIssuesFiles = fileIds(SpaceLendingFileKind.REPORT_ISSUES),
        paymentProofs = fileIds(SpaceLendingFileKind.PAYMENT_PROOF),
    )

    companion object : UuidEntityClass<SpaceLendingEntity>(SpaceLendings)
}
