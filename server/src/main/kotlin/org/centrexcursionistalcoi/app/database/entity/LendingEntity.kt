package org.centrexcursionistalcoi.app.database.entity

import java.util.UUID
import org.centrexcursionistalcoi.app.database.table.LendingItems
import org.centrexcursionistalcoi.app.database.table.Lendings
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass

class LendingEntity(id: EntityID<UUID>): UUIDEntity(id) {
    companion object : UUIDEntityClass<LendingEntity>(Lendings)

    var userSub by UserReferenceEntity referencedOn Lendings.userSub
    var timestamp by Lendings.timestamp
    var confirmed by Lendings.confirmed
    val items by InventoryItemEntity via LendingItems
}
