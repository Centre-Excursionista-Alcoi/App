package org.centrexcursionistalcoi.app.database.entity

import java.util.UUID
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid
import org.centrexcursionistalcoi.app.data.InventoryItem
import org.centrexcursionistalcoi.app.database.base.EntityPatcher
import org.centrexcursionistalcoi.app.database.table.InventoryItems
import org.centrexcursionistalcoi.app.request.UpdateInventoryItemRequest
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

class InventoryItemEntity(id: EntityID<UUID>) : UUIDEntity(id), EntityDataConverter<InventoryItem, Uuid>, EntityPatcher<UpdateInventoryItemRequest> {
    companion object : UUIDEntityClass<InventoryItemEntity>(InventoryItems)

    var variation by InventoryItems.variation
    var type by InventoryItemTypeEntity referencedOn InventoryItems.type

    context(_: JdbcTransaction)
    override fun toData(): InventoryItem = InventoryItem(
        id = id.value.toKotlinUuid(),
        variation = variation,
        type = type.id.value.toKotlinUuid(),
    )

    context(_: JdbcTransaction)
    override fun patch(request: UpdateInventoryItemRequest) {
        request.variation?.let { variation = it }
        request.type?.let { type = InventoryItemTypeEntity[it.toJavaUuid()] }
    }
}
