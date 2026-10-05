package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.data.Category
import org.centrexcursionistalcoi.app.data.SpaceLending
import org.centrexcursionistalcoi.app.data.SpaceLendingFileKind
import org.centrexcursionistalcoi.app.data.SpaceLendingKey
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.base.LastUpdateEntity
import org.centrexcursionistalcoi.app.database.table.SpaceLendingFiles
import org.centrexcursionistalcoi.app.database.table.SpaceLendingKeyRequests
import org.centrexcursionistalcoi.app.database.table.SpaceLendingKeys
import org.centrexcursionistalcoi.app.database.table.SpaceLendings
import org.centrexcursionistalcoi.app.now
import org.centrexcursionistalcoi.app.routes.helper.notifyUpdateForEntity
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.inList
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

    /** How many keys of each type this lending asks for. */
    context(_: JdbcTransaction)
    fun requestedKeys(): Map<Uuid, Int> = preloadedRequestedKeys
        ?: SpaceLendingKeyRequests.selectAll().where { SpaceLendingKeyRequests.lending eq id }
            .associate { it[SpaceLendingKeyRequests.keyType].value to it[SpaceLendingKeyRequests.quantity] }

    /** The keys handed out to this lending, returned or not. */
    context(_: JdbcTransaction)
    fun keys(): List<SpaceLendingKey> = preloadedKeys
        ?: SpaceLendingKeys.selectAll().where { SpaceLendingKeys.lending eq id }.map { it.toKey() }

    context(_: JdbcTransaction)
    fun fileIds(kind: SpaceLendingFileKind): List<Uuid> = preloadedFiles?.get(kind).orEmpty().takeIf { preloadedFiles != null }
        ?: SpaceLendingFiles.selectAll()
            .where { (SpaceLendingFiles.lending eq id) and (SpaceLendingFiles.kind eq kind) }
            .map { it[SpaceLendingFiles.file].value }

    /** The keys and files, loaded ahead for a whole list, see [withDataPreloaded]. */
    private var preloadedKeys: List<SpaceLendingKey>? = null
    private var preloadedRequestedKeys: Map<Uuid, Int>? = null
    private var preloadedFiles: Map<SpaceLendingFileKind, List<Uuid>>? = null

    context(_: JdbcTransaction)
    fun files(kind: SpaceLendingFileKind): List<FileEntity> = fileIds(kind).mapNotNull { FileEntity.findById(it) }

    context(_: JdbcTransaction)
    override fun toData(): SpaceLending = SpaceLending(
        id = id.value,
        timestamp = timestamp,
        space = SpaceLendings.space.lookup().value,
        userSub = SpaceLendings.userSub.lookup()?.value,
        checkIn = checkIn,
        checkOut = checkOut,
        attendees = attendees,
        acceptedConditionsAt = acceptedConditionsAt,
        cancelled = cancelled,
        notes = notes,
        pickedUpAt = pickedUpAt,
        pickedUpBy = SpaceLendings.pickedUpBy.lookup()?.value,
        returnedAt = returnedAt,
        returnedBy = SpaceLendings.returnedBy.lookup()?.value,
        requestedKeys = requestedKeys(),
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

    companion object : UuidEntityClass<SpaceLendingEntity>(SpaceLendings) {
        private fun ResultRow.toKey() = SpaceLendingKey(
            key = this[SpaceLendingKeys.key].value,
            givenBy = this[SpaceLendingKeys.givenBy]?.value,
            givenAt = this[SpaceLendingKeys.givenAt],
            returnedTo = this[SpaceLendingKeys.returnedTo]?.value,
            returnedAt = this[SpaceLendingKeys.returnedAt],
        )

        /**
         * Loads, for all of [lendings] at once, their keys (asked for and handed out) and files. Instead, each would run its own queries. Only
         * holds while the caller stays in the current transaction.
         */
        context(_: JdbcTransaction)
        fun withDataPreloaded(lendings: List<SpaceLendingEntity>): List<SpaceLendingEntity> {
            if (lendings.isEmpty()) return lendings
            val ids = lendings.map { it.id }
            val keys = SpaceLendingKeys.selectAll().where { SpaceLendingKeys.lending inList ids }
                .groupBy({ it[SpaceLendingKeys.lending].value }, { it.toKey() })
            val requested = SpaceLendingKeyRequests.selectAll().where { SpaceLendingKeyRequests.lending inList ids }
                .groupBy({ it[SpaceLendingKeyRequests.lending].value }, { it[SpaceLendingKeyRequests.keyType].value to it[SpaceLendingKeyRequests.quantity] })
            val files = SpaceLendingFiles.selectAll().where { SpaceLendingFiles.lending inList ids }
                .groupBy({ it[SpaceLendingFiles.lending].value }, { it[SpaceLendingFiles.kind] to it[SpaceLendingFiles.file].value })
            for (lending in lendings) {
                lending.preloadedKeys = keys[lending.id.value].orEmpty()
                lending.preloadedRequestedKeys = requested[lending.id.value].orEmpty().toMap()
                lending.preloadedFiles = SpaceLendingFileKind.entries.associateWith { kind ->
                    files[lending.id.value].orEmpty().filter { it.first == kind }.map { it.second }
                }
            }
            return lendings
        }
    }
}
