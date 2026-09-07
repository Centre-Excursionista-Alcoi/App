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
    val memory: ReferencedMemory?,
    val memoryReviewed: Boolean,

    val from: LocalDate,
    val to: LocalDate,
    val notes: String?,
    val items: List<ReferencedInventoryItem>,
) : ReferencedEntity<Uuid, Lending> {

   override fun dereference() = Lending(
       id = id,
       userSub = user.sub,
       timestamp = timestamp,
       confirmed = confirmed,
       taken = taken,
       givenBy = givenBy?.sub,
       givenAt = givenAt,
       returned = returned,
       receivedItems = receivedItems,
       memorySubmitted = memorySubmitted,
       memorySubmittedAt = memorySubmittedAt,
       memory = memory?.id,
       memoryReviewed = memoryReviewed,
       from = from,
       to = to,
       notes = notes,
       items = items.map { it.dereference() },
   )

   val durationDays: Int = from.daysUntil(to) + 1

   fun status(): Status = dereference().status()
}
