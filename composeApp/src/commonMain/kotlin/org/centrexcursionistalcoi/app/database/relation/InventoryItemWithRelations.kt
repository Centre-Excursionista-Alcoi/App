package org.centrexcursionistalcoi.app.database.relation

import androidx.room3.Embedded
import androidx.room3.Relation
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem.Companion.referenced
import org.centrexcursionistalcoi.app.database.room.entity.InventoryItemEntity
import org.centrexcursionistalcoi.app.database.room.entity.InventoryItemTypeEntity
import org.centrexcursionistalcoi.app.exception.MissingCrossReferenceException

data class InventoryItemWithRelations(
    @Embedded val item: InventoryItemEntity,
    // Nullable even though `type` is a required, cascading FK: Room can't express "exactly one" for a @Relation,
    // only "zero or more" -- the mapper treats a null here as a genuine data-integrity error.
    @Relation(entity = InventoryItemTypeEntity::class, parentColumns = ["type"], entityColumns = ["id"])
    val type: InventoryItemTypeWithRelations?,
)

fun InventoryItemWithRelations.toReferenced(): ReferencedInventoryItem {
    val referencedType = type?.toReferenced() ?: throw MissingCrossReferenceException("InventoryItemType", item.type)
    return item.toInventoryItem().referenced(referencedType)
}
