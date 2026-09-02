package org.centrexcursionistalcoi.app.data

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.serializer.InstantSerializer
import kotlin.time.Instant
import kotlin.uuid.Uuid

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
    val receivedItems: List<ReceivedItem>,

    val memorySubmitted: Boolean,
    @Serializable(InstantSerializer::class) val memorySubmittedAt: Instant?,
    /** The id of the linked memory, if any. Memories are their own resource, fetched separately. */
    val memory: Uuid?,
    val memoryReviewed: Boolean,

    val from: LocalDate,
    val to: LocalDate,
    val notes: String?,
    val items: List<InventoryItem>,
): Entity<Uuid> {
    enum class Status {
        REQUESTED,
        CONFIRMED,
        TAKEN,
        RETURNED,
        MEMORY_SUBMITTED,
        COMPLETE;

        /**
         * Checks whether the status is pending. This is: [REQUESTED], [CONFIRMED], [TAKEN] or [RETURNED].
         */
        fun isPending() = this in listOf(REQUESTED, CONFIRMED, TAKEN, RETURNED)

        /**
         * Lendings can only be canceled if they are in [REQUESTED] or [CONFIRMED] status.
         */
        fun canBeCancelled() = this in listOf(REQUESTED, CONFIRMED)
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
        "memorySubmitted" to memorySubmitted,
        "memorySubmittedAt" to memorySubmittedAt,
        "memory" to memory,
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
}
