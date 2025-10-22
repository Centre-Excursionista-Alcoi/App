package org.centrexcursionistalcoi.app.data

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.data.Lending.Status
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem.Companion.referenced
import org.centrexcursionistalcoi.app.serializer.InstantSerializer

@Serializable
data class ReferencedLending(
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
    val items: List<ReferencedInventoryItem>,

    override val referencedEntity: Lending
): ReferencedEntity<Uuid, Lending>(), DocumentFileContainer {
    companion object {
        fun Lending.referenced(inventoryItemTypes: List<InventoryItemType>) = ReferencedLending(
            id = this.id,
            userSub = this.userSub,
            timestamp = this.timestamp,
            confirmed = this.confirmed,
            taken = this.taken,
            givenBy = this.givenBy,
            givenAt = this.givenAt,
            returned = this.returned,
            receivedBy = this.receivedBy,
            receivedAt = this.receivedAt,
            memorySubmitted = this.memorySubmitted,
            memorySubmittedAt = this.memorySubmittedAt,
            memoryDocument = this.memoryDocument,
            memoryReviewed = this.memoryReviewed,
            from = this.from,
            to = this.to,
            notes = this.notes,
            items = this.items.map { item -> item.referenced(inventoryItemTypes.first { it.id == item.type }) },
            referencedEntity = this,
        )
    }

    fun status(): Status = referencedEntity.status()

    override val files: Map<String, Uuid?> = referencedEntity.files

    override val documentFile: Uuid? = referencedEntity.documentFile
}
