package org.centrexcursionistalcoi.app.database.relationship

import androidx.room.Embedded
import androidx.room.Relation
import org.centrexcursionistalcoi.app.database.entity.Item
import org.centrexcursionistalcoi.app.database.entity.ItemType

data class ItemTypeWithItems(
    @Embedded val itemType: ItemType,
    @Relation(
        parentColumn = "id",
        entityColumn = "itemTypeId"
    )
    val items: List<Item>
)
