package org.centrexcursionistalcoi.app.data

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.serializer.InstantSerializer

@Serializable
data class Lending(
    override val id: Uuid,
    val userSub: String,
    @Serializable(InstantSerializer::class) val timestamp: Instant,
    val confirmed: Boolean,

    val taken: Boolean,
    val givenBy: String?,
    @Serializable(InstantSerializer::class) val givenAt: Instant?,

    val returned: Boolean,
    val receivedBy: String?,
    @Serializable(InstantSerializer::class) val receivedAt: Instant?,

    val memorySubmitted: Boolean,
    @Serializable(InstantSerializer::class) val memorySubmittedAt: Instant?,
    val memoryDocument: Uuid?,
    val memoryReviewed: Boolean,

    val from: LocalDate,
    val to: LocalDate,
    val notes: String?,
    val items: List<InventoryItem>,
): Entity<Uuid>, DocumentFileContainer {
    enum class Status {
        REQUESTED,
        CONFIRMED,
        TAKEN,
        RETURNED,
        MEMORY_SUBMITTED,
        COMPLETE,
    }

    override fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "userSub" to userSub,
        "timestamp" to timestamp,
        "confirmed" to confirmed,
        "taken" to taken,
        "givenBy" to givenBy,
        "givenAt" to givenAt,
        "returned" to returned,
        "receivedBy" to receivedBy,
        "receivedAt" to receivedAt,
        "memorySubmitted" to memorySubmitted,
        "memorySubmittedAt" to memorySubmittedAt,
        "memoryDocument" to memoryDocument,
        "memoryReviewed" to memoryReviewed,
        "from" to from,
        "to" to to,
        "notes" to notes,
        "items" to items,
    )

    fun status(): Status = when {
        memoryReviewed -> Status.COMPLETE
        memorySubmitted && !memoryReviewed -> Status.MEMORY_SUBMITTED
        returned && !memorySubmitted -> Status.RETURNED
        taken && !returned -> Status.TAKEN
        confirmed && !taken -> Status.CONFIRMED
        else -> Status.REQUESTED
    }

    override val files: Map<String, Uuid?> = mapOf(
        "memoryDocument" to memoryDocument
    )

    override val documentFile: Uuid? = memoryDocument
}
