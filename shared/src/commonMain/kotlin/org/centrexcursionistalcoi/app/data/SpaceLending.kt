package org.centrexcursionistalcoi.app.data

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.serializer.InstantSerializer
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class SpaceLending(
    override val id: Uuid,
    @Serializable(InstantSerializer::class) val timestamp: Instant,

    val space: Uuid,
    /** The user who made the lending. Null if they were deleted. */
    val userSub: String?,

    val checkIn: LocalDate,
    val checkOut: LocalDate,

    val attendees: Map<Category, Int>,
    @Serializable(InstantSerializer::class) val acceptedConditionsAt: Instant?,
    val cancelled: Boolean,
    val notes: String?,

    /** When a manager handed the keys (and whatever else was needed) over. From then on, the lending is locked. */
    @Serializable(InstantSerializer::class) val pickedUpAt: Instant?,
    val pickedUpBy: String?,
    /** When a manager took the keys back. */
    @Serializable(InstantSerializer::class) val returnedAt: Instant?,
    val returnedBy: String?,

    /** How many keys of each [SpaceKeyType] the lending asked for. */
    val requestedKeys: Map<Uuid, Int>,
    /** The keys handed out, in the end (they are chosen when a manager hands them over). */
    val keys: List<SpaceLendingKey>,

    val totalPrice: Double,
    val paymentStatus: PaymentStatus,

    val reportNotes: String?,
    val reportIssues: String?,
    @Serializable(InstantSerializer::class) val reportSubmittedAt: Instant?,

    /** Ids of the files of the report (notes). */
    val reportNotesFiles: List<Uuid>,
    /** Ids of the files of the report (issues). */
    val reportIssuesFiles: List<Uuid>,
    /** Ids of the payment proofs. */
    val paymentProofs: List<Uuid>,
) : Entity<Uuid>

/**
 * The dates of a space that are taken. Doesn't tell who by.
 */
@Serializable
data class SpaceOccupancy(
    val checkIn: LocalDate,
    val checkOut: LocalDate,
)
