package org.centrexcursionistalcoi.app.database.room.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import org.centrexcursionistalcoi.app.data.InventoryItemType
import kotlin.uuid.Uuid

@Entity(
    tableName = "InventoryItemTypes",
    foreignKeys = [
        ForeignKey(
            entity = DepartmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["department"],
        ),
    ],
    indices = [Index(value = ["department"])],
)
data class InventoryItemTypeEntity(
    @PrimaryKey
    val id: Uuid,
    val displayName: String,
    val description: String?,
    val categories: List<String>?,
    val department: Uuid?,
    val image: Uuid?,
) {
    /** [InventoryItemType.weight] isn't persisted; it's always `null` when read back from the database. */
    fun toInventoryItemType() = InventoryItemType(
        id = id,
        displayName = displayName,
        description = description,
        categories = categories,
        department = department,
        image = image,
    )

    companion object {
        fun InventoryItemType.toEntity() = InventoryItemTypeEntity(
            id = id,
            displayName = displayName,
            description = description,
            categories = categories,
            department = department,
            image = image,
        )
    }
}
