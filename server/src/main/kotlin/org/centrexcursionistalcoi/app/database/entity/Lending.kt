package org.centrexcursionistalcoi.app.database.entity

import java.time.ZoneOffset
import org.centrexcursionistalcoi.app.data.ItemLendingD
import org.centrexcursionistalcoi.app.database.common.BookingEntity
import org.centrexcursionistalcoi.app.database.table.LendingItemsTable
import org.centrexcursionistalcoi.app.database.table.LendingsTable
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Lending(id: EntityID<Int>) : BookingEntity(id) {
    companion object : IntEntityClass<Lending>(LendingsTable)

    val createdAt by LendingsTable.createdAt

    override var user by User referencedOn LendingsTable.user

    override var confirmed by LendingsTable.confirmed

    override var from by LendingsTable.from
    override var to by LendingsTable.to

    override var takenAt by LendingsTable.takenAt
    override var returnedAt by LendingsTable.returnedAt

    fun serializable() = ItemLendingD(
        id = id.value,
        createdAt = createdAt.toEpochMilli(),
        userId = user.id.value,
        confirmed = confirmed,
        from = from.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        to = to.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        takenAt = takenAt?.toEpochMilli(),
        returnedAt = returnedAt?.toEpochMilli(),
        itemIds = LendingItem
            .find { LendingItemsTable.lending eq id }
            .map { it.item.id.value }
            .toSet()
    )
}
