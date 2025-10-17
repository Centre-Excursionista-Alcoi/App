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
    val from: LocalDate,
    val to: LocalDate,
    val notes: String?,
    val items: List<InventoryItem>,
): Entity<Uuid> {
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
        "from" to from,
        "to" to to,
        "notes" to notes,
        "items" to items,
    )
}
