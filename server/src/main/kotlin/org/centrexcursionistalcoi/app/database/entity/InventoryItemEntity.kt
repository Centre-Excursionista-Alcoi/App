package org.centrexcursionistalcoi.app.database.entity

import java.util.UUID
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid
import org.centrexcursionistalcoi.app.data.InventoryItem
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.base.EntityPatcher
import org.centrexcursionistalcoi.app.database.entity.base.LastUpdateEntity
import org.centrexcursionistalcoi.app.database.table.InventoryItems
import org.centrexcursionistalcoi.app.now
import org.centrexcursionistalcoi.app.request.UpdateInventoryItemRequest
import org.centrexcursionistalcoi.app.routes.helper.notifyUpdateForEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

class InventoryItemEntity(id: EntityID<UUID>) : UUIDEntity(id), LastUpdateEntity, EntityDataConverter<InventoryItem, Uuid>, EntityPatcher<UpdateInventoryItemRequest> {
    companion object : UUIDEntityClass<InventoryItemEntity>(InventoryItems)

    override var lastUpdate by InventoryItems.lastUpdate

    var variation by InventoryItems.variation
    var type by InventoryItemTypeEntity referencedOn InventoryItems.type
    var nfcId by InventoryItems.nfcId
    var manufacturerTraceabilityCode by InventoryItems.manufacturerTraceabilityCode

    context(_: JdbcTransaction)
    override fun toData(): InventoryItem = InventoryItem(
        id = id.value.toKotlinUuid(),
        variation = variation,
        type = type.id.value.toKotlinUuid(),
        nfcId = nfcId,
        manufacturerTraceabilityCode = manufacturerTraceabilityCode,
    )

    context(_: JdbcTransaction)
    override fun patch(request: UpdateInventoryItemRequest) {
        request.variation?.let { variation = it.takeUnless { it.isEmpty() } }
        request.type?.let { type = InventoryItemTypeEntity[it.toJavaUuid()] }
        request.nfcId?.let { nfcId = it.takeUnless { it.isEmpty() } }
        request.manufacturerTraceabilityCode?.let { manufacturerTraceabilityCode = it.takeUnless { it.isEmpty() } }
    }

    override suspend fun updated() {
        notifyUpdateForEntity(Companion, id)
        Database { lastUpdate = now() }
    }
}
