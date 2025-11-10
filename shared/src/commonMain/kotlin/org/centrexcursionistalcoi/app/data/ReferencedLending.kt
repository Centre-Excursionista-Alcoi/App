package org.centrexcursionistalcoi.app.data

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.InventoryItemType.Companion.getType
import org.centrexcursionistalcoi.app.data.Lending.Status
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem.Companion.referenced
import org.centrexcursionistalcoi.app.data.UserData.Companion.getUser
import org.centrexcursionistalcoi.app.serializer.InstantSerializer

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
    val memoryDocument: Uuid?,
    val memoryPlainText: String?,
    val memoryReviewed: Boolean,

    val from: LocalDate,
    val to: LocalDate,
    val notes: String?,
    val items: List<ReferencedInventoryItem>,

    override val referencedEntity: Lending
): ReferencedEntity<Uuid, Lending>(), DocumentFileContainer {
    companion object {
        fun Lending.referenced(users: List<UserData>, inventoryItemTypes: List<InventoryItemType>) = ReferencedLending(
            id = this.id,
            user = users.getUser(userSub),
            timestamp = this.timestamp,
            confirmed = this.confirmed,
            taken = this.taken,
            givenBy = this.givenBy?.let { givenBy -> users.getUser(givenBy) },
            givenAt = this.givenAt,
            returned = this.returned,
            receivedItems = receivedItems,
            memorySubmitted = this.memorySubmitted,
            memorySubmittedAt = this.memorySubmittedAt,
            memoryDocument = this.memoryDocument,
            memoryPlainText = this.memoryPlainText,
            memoryReviewed = this.memoryReviewed,
            from = this.from,
            to = this.to,
            notes = this.notes,
            items = this.items.map { item -> item.referenced(inventoryItemTypes.getType(item.type)) },
            referencedEntity = this,
        )
    }

    val durationDays: Int
        get() = to.daysUntil(from) + 1

    fun status(): Status = referencedEntity.status()

    override val files: Map<String, Uuid?> = referencedEntity.files

    override val documentFile: Uuid? = referencedEntity.documentFile
}
