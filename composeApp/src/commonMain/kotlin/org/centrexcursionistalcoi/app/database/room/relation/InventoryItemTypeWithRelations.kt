package org.centrexcursionistalcoi.app.database.room.relation

import androidx.room3.Embedded
import androidx.room3.Relation
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItemType
import org.centrexcursionistalcoi.app.database.room.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.room.entity.InventoryItemTypeEntity

data class InventoryItemTypeWithRelations(
    @Embedded val type: InventoryItemTypeEntity,
    @Relation(parentColumns = ["department"], entityColumns = ["id"])
    val department: DepartmentEntity?,
)

fun InventoryItemTypeWithRelations.toReferenced(): ReferencedInventoryItemType {
    // InventoryItemType.weight isn't persisted; it's always null when read back from the database.
    val domainType = type.toInventoryItemType()
    return ReferencedInventoryItemType(
        id = type.id,
        displayName = type.displayName,
        description = type.description,
        categories = type.categories,
        weight = domainType.weight,
        department = department?.toDepartment(),
        image = type.image,
    )
}
