package org.centrexcursionistalcoi.app.database.entity

import java.util.UUID
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.base.EntityPatcher
import org.centrexcursionistalcoi.app.database.entity.base.ImageContainerEntity
import org.centrexcursionistalcoi.app.database.entity.base.LastUpdateEntity
import org.centrexcursionistalcoi.app.database.table.InventoryItemTypes
import org.centrexcursionistalcoi.app.now
import org.centrexcursionistalcoi.app.request.UpdateInventoryItemTypeRequest
import org.centrexcursionistalcoi.app.routes.helper.notifyUpdateForEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

class InventoryItemTypeEntity(id: EntityID<UUID>): UUIDEntity(id), LastUpdateEntity, EntityDataConverter<InventoryItemType, Uuid>, EntityPatcher<UpdateInventoryItemTypeRequest>, ImageContainerEntity {
    companion object : UUIDEntityClass<InventoryItemTypeEntity>(InventoryItemTypes)

    override var lastUpdate by InventoryItemTypes.lastUpdate

    var displayName by InventoryItemTypes.displayName
    var description by InventoryItemTypes.description
    var categories by InventoryItemTypes.categories

    var department by DepartmentEntity optionalReferencedOn InventoryItemTypes.department

    override var image by FileEntity optionalReferencedOn InventoryItemTypes.image

    context(_: JdbcTransaction)
    override fun toData(): InventoryItemType = InventoryItemType(
        id = id.value.toKotlinUuid(),
        displayName = displayName,
        description = description,
        categories = categories,
        department = department?.id?.value?.toKotlinUuid(),
        image = image?.id?.value?.toKotlinUuid()
    )

    context(_: JdbcTransaction)
    override fun patch(request: UpdateInventoryItemTypeRequest) {
        request.displayName?.let { displayName = it }
        request.description?.let { description = it.takeUnless { value -> value.isBlank() } }
        request.categories?.let { categories = it }
        request.department?.let { department = DepartmentEntity.findById(it.toJavaUuid()) }
        updateOrSetImage(request.image)
    }

    override suspend fun updated() {
        notifyUpdateForEntity(Companion, id)
        Database { lastUpdate = now() }
    }
}
