package org.centrexcursionistalcoi.app.database.room.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlinx.datetime.LocalDate
import org.centrexcursionistalcoi.app.data.InventoryItem
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.data.ReceivedItem
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Entity(tableName = "Lendings")
data class LendingEntity(
    @PrimaryKey
    val id: Uuid,
    val userSub: String,
    val timestamp: Instant,
    val fromDate: LocalDate,
    val toDate: LocalDate,
    val confirmed: Boolean,
    val taken: Boolean,
    val givenBy: String?,
    val givenAt: Instant?,
    val returned: Boolean,
    val memorySubmitted: Boolean,
    val memorySubmittedAt: Instant?,
    val memoryReviewed: Boolean,
    val notes: String?,
) {
    /**
     * Builds the base [Lending], given its already-resolved [items] and [receivedItems] (from the `LendingItems`/
     * `ReceivedItems` join tables) and the id of its linked memory, if any.
     */
    fun toLending(items: List<InventoryItem>, receivedItems: List<ReceivedItem>, memoryId: Uuid?) = Lending(
        id = id,
        userSub = userSub,
        timestamp = timestamp,
        confirmed = confirmed,
        taken = taken,
        givenBy = givenBy,
        givenAt = givenAt,
        returned = returned,
        receivedItems = receivedItems,
        memorySubmitted = memorySubmitted,
        memorySubmittedAt = memorySubmittedAt,
        memory = memoryId,
        memoryReviewed = memoryReviewed,
        from = fromDate,
        to = toDate,
        notes = notes,
        items = items,
    )

    companion object {
        /** Persists only [Lending]'s flat fields; [Lending.items] and [Lending.receivedItems] are stored separately. */
        fun Lending.toEntity() = LendingEntity(
            id = id,
            userSub = userSub,
            timestamp = timestamp,
            fromDate = from,
            toDate = to,
            confirmed = confirmed,
            taken = taken,
            givenBy = givenBy,
            givenAt = givenAt,
            returned = returned,
            memorySubmitted = memorySubmitted,
            memorySubmittedAt = memorySubmittedAt,
            memoryReviewed = memoryReviewed,
            notes = notes,
        )
    }
}
