package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.data.InventoryItem
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.base.EntityPatcher
import org.centrexcursionistalcoi.app.database.entity.base.LastUpdateEntity
import org.centrexcursionistalcoi.app.database.table.InventoryItems
import org.centrexcursionistalcoi.app.now
import org.centrexcursionistalcoi.app.request.UpdateInventoryItemRequest
import org.centrexcursionistalcoi.app.routes.helper.notifyUpdateForEntity
import org.centrexcursionistalcoi.app.security.UserSession
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import kotlin.uuid.Uuid

class InventoryItemEntity(id: EntityID<Uuid>) : UuidEntity(id), LastUpdateEntity, EntityDataConverter<InventoryItem, Uuid>, EntityPatcher<UpdateInventoryItemRequest> {
    companion object : UuidEntityClass<InventoryItemEntity>(InventoryItems)

    /**
     * Whether this single item is visible to [session] -- delegates entirely to its type's own
     * [InventoryItemTypeEntity.isVisibleTo], since an item's visibility is defined purely by its type's
     * department (see the `listProvider` in `InventoryRoutes.kt`).
     */
    context(_: JdbcTransaction)
    fun isVisibleTo(session: UserSession?): Boolean = type.isVisibleTo(session)

    override var lastUpdate by InventoryItems.lastUpdate

    var variation by InventoryItems.variation
    var type by InventoryItemTypeEntity referencedOn InventoryItems.type
    var nfcId by InventoryItems.nfcId
    var manufacturerTraceabilityCode by InventoryItems.manufacturerTraceabilityCode

    context(_: JdbcTransaction)
    override fun toData(): InventoryItem = InventoryItem(
        id = id.value,
        variation = variation,
        type = InventoryItems.type.lookup().value,
        nfcId = nfcId,
        manufacturerTraceabilityCode = manufacturerTraceabilityCode,
    )

    context(_: JdbcTransaction)
    override fun patch(request: UpdateInventoryItemRequest) {
        request.variation?.let { variation = it.takeUnless { it.isEmpty() } }
        request.type?.let { type = InventoryItemTypeEntity[it] }
        request.nfcId?.let { nfcId = it.takeUnless { it.isEmpty() } }
        request.manufacturerTraceabilityCode?.let { manufacturerTraceabilityCode = it.takeUnless { it.isEmpty() } }
    }

    override suspend fun updated() {
        notifyUpdateForEntity(Companion, id)
        Database { lastUpdate = now() }
    }
}
