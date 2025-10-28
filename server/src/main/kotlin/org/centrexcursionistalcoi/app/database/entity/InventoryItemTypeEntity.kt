package org.centrexcursionistalcoi.app.database.entity

import java.util.UUID
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.database.base.EntityPatcher
import org.centrexcursionistalcoi.app.database.entity.base.ImageContainerEntity
import org.centrexcursionistalcoi.app.database.table.InventoryItemTypes
import org.centrexcursionistalcoi.app.request.UpdateInventoryItemTypeRequest
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

class InventoryItemTypeEntity(id: EntityID<UUID>): UUIDEntity(id), EntityDataConverter<InventoryItemType, Uuid>, EntityPatcher<UpdateInventoryItemTypeRequest>, ImageContainerEntity {
    companion object : UUIDEntityClass<InventoryItemTypeEntity>(InventoryItemTypes)

    var displayName by InventoryItemTypes.displayName
    var description by InventoryItemTypes.description
    var category by InventoryItemTypes.category
    override var image by FileEntity optionalReferencedOn InventoryItemTypes.image

    context(_: JdbcTransaction)
    override fun toData(): InventoryItemType = InventoryItemType(
        id = id.value.toKotlinUuid(),
        displayName = displayName,
        description = description,
        category = category,
        image = image?.id?.value?.toKotlinUuid()
    )

    context(_: JdbcTransaction)
    override fun patch(request: UpdateInventoryItemTypeRequest) {
        request.displayName?.let { displayName = it }
        request.description?.let { description = it }
        request.category?.let { category = it }
        updateOrSetImage(request.image, "inventory_${id.value}_image")
    }
}
