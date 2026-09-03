package org.centrexcursionistalcoi.app.database.room.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import org.centrexcursionistalcoi.app.data.ReceivedItem
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Entity(
    tableName = "ReceivedItems",
    foreignKeys = [
        ForeignKey(
            entity = LendingEntity::class,
            parentColumns = ["id"],
            childColumns = ["lending"],
        ),
        ForeignKey(
            entity = InventoryItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["item"],
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["sub"],
            childColumns = ["receivedBy"],
        ),
    ],
    indices = [Index(value = ["lending"]), Index(value = ["item"]), Index(value = ["receivedBy"])],
)
data class ReceivedItemEntity(
    @PrimaryKey
    val id: Uuid,
    val lending: Uuid,
    val item: Uuid,
    val notes: String?,
    val receivedBy: String,
    val receivedAt: Instant,
) {
    fun toReceivedItem() = ReceivedItem(
        id = id,
        lendingId = lending,
        itemId = item,
        notes = notes,
        receivedBy = receivedBy,
        receivedAt = receivedAt,
    )

    companion object {
        fun ReceivedItem.toEntity() = ReceivedItemEntity(
            id = id,
            lending = lendingId,
            item = itemId,
            notes = notes,
            receivedBy = receivedBy,
            receivedAt = receivedAt,
        )
    }
}
