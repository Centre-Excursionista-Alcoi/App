package org.centrexcursionistalcoi.app.database.entity

import java.util.UUID
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid
import org.centrexcursionistalcoi.app.data.InventoryItem
import org.centrexcursionistalcoi.app.database.table.InventoryItems
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

class InventoryItemEntity(id: EntityID<UUID>) : UUIDEntity(id), EntityDataConverter<InventoryItem, Uuid> {
    companion object : UUIDEntityClass<InventoryItemEntity>(InventoryItems)

    var variation by InventoryItems.variation
    var type by InventoryItemTypeEntity referencedOn InventoryItems.type

    context(_: JdbcTransaction)
    override fun toData(): InventoryItem = InventoryItem(
        id = id.value.toKotlinUuid(),
        variation = variation,
        type = type.id.value.toKotlinUuid(),
    )
}
