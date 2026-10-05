package org.centrexcursionistalcoi.app.database.entity

import kotlin.uuid.Uuid
import kotlin.time.toKotlinInstant
import org.centrexcursionistalcoi.app.data.ReceivedItem
import org.centrexcursionistalcoi.app.database.table.ReceivedItems
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

class ReceivedItemEntity(id: EntityID<Uuid>): UuidEntity(id) {
    companion object : UuidEntityClass<ReceivedItemEntity>(ReceivedItems)

    var lending by LendingEntity referencedOn ReceivedItems.lending
    var item by InventoryItemEntity referencedOn ReceivedItems.item

    var notes by ReceivedItems.notes

    var receivedBy by UserReferenceEntity referencedOn ReceivedItems.receivedBy
    var receivedAt by ReceivedItems.receivedAt

    context(_: JdbcTransaction)
    fun toReceivedItem(): ReceivedItem = ReceivedItem(
        id = this.id.value,
        lendingId = ReceivedItems.lending.lookup().value,
        itemId = ReceivedItems.item.lookup().value,
        notes = notes,
        receivedBy = ReceivedItems.receivedBy.lookup().value,
        receivedAt = receivedAt,
    )
}
