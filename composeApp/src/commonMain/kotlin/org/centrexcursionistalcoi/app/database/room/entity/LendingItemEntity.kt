package org.centrexcursionistalcoi.app.database.room.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import kotlin.uuid.Uuid

@Entity(
    tableName = "LendingItems",
    primaryKeys = ["lendingId", "itemId"],
    foreignKeys = [
        ForeignKey(
            entity = LendingEntity::class,
            parentColumns = ["id"],
            childColumns = ["lendingId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = InventoryItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["itemId"])],
)
data class LendingItemEntity(
    val lendingId: Uuid,
    val itemId: Uuid,
)
