package org.centrexcursionistalcoi.app.database.entity

import java.util.UUID
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.database.table.InventoryItemTypes
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

class InventoryItemTypeEntity(id: EntityID<UUID>): UUIDEntity(id), EntityDataConverter<InventoryItemType, Uuid> {
    companion object : UUIDEntityClass<InventoryItemTypeEntity>(InventoryItemTypes)

    var displayName by InventoryItemTypes.displayName
    var description by InventoryItemTypes.description
    var image by FileEntity optionalReferencedOn InventoryItemTypes.image

    context(_: JdbcTransaction)
    override fun toData(): InventoryItemType = InventoryItemType(
        id = id.value.toKotlinUuid(),
        displayName = displayName,
        description = description,
        image = image?.id?.value?.toKotlinUuid()
    )
}
