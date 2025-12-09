package org.centrexcursionistalcoi.app.data

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.Lending.Status
import org.centrexcursionistalcoi.app.serializer.InstantSerializer
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class ReferencedLending(
    override val id: Uuid,
    val user: UserData,
    @Serializable(InstantSerializer::class) val timestamp: Instant,
    val confirmed: Boolean,

    val taken: Boolean,
    val givenBy: UserData?,
    @Serializable(InstantSerializer::class) val givenAt: Instant?,

    val returned: Boolean,
    val receivedItems: List<ReceivedItem>,

    val memorySubmitted: Boolean,
    @Serializable(InstantSerializer::class) val memorySubmittedAt: Instant?,
    val memory: LendingMemory?,
    val memoryPdf: Uuid?,
    val memoryReviewed: Boolean,

    val from: LocalDate,
    val to: LocalDate,
    val notes: String?,
    val items: List<ReferencedInventoryItem>,

    override val referencedEntity: Lending
) : ReferencedEntity<Uuid, Lending>(), FileContainer, SubReferencedFileContainer {

    val durationDays: Int = from.daysUntil(to) + 1

    fun status(): Status = referencedEntity.status()

    override val files: Map<String, Uuid?> = mapOf(
        "memoryPdf" to memoryPdf
    )

    override val referencedFiles: List<Triple<String, Uuid?, String>> get() = referencedEntity.referencedFiles
}
