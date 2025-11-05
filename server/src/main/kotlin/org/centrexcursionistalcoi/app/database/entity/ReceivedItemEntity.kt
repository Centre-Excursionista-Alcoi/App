package org.centrexcursionistalcoi.app.database.entity

import java.util.UUID
import kotlin.time.toKotlinInstant
import kotlin.uuid.toKotlinUuid
import org.centrexcursionistalcoi.app.data.ReceivedItem
import org.centrexcursionistalcoi.app.database.table.ReceivedItems
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

class ReceivedItemEntity(id: EntityID<UUID>): UUIDEntity(id) {
    companion object : UUIDEntityClass<ReceivedItemEntity>(ReceivedItems)

    var lending by LendingEntity referencedOn ReceivedItems.lending
    var item by InventoryItemEntity referencedOn ReceivedItems.item

    var notes by ReceivedItems.notes

    var receivedBy by UserReferenceEntity referencedOn ReceivedItems.receivedBy
    var receivedAt by ReceivedItems.receivedAt

    context(_: JdbcTransaction)
    fun toReceivedItem(): ReceivedItem = ReceivedItem(
        id = this.id.value.toKotlinUuid(),
        lendingId = lending.id.value.toKotlinUuid(),
        itemId = item.id.value.toKotlinUuid(),
        notes = notes,
        receivedBy = receivedBy.sub.value,
        receivedAt = receivedAt.toKotlinInstant(),
    )
}
