package org.centrexcursionistalcoi.app.database.entity

import kotlinx.datetime.toKotlinInstant
import kotlinx.datetime.toKotlinLocalDate
import org.centrexcursionistalcoi.app.data.ItemLendingD
import org.centrexcursionistalcoi.app.database.common.BookingEntity
import org.centrexcursionistalcoi.app.database.table.LendingItemsTable
import org.centrexcursionistalcoi.app.database.table.LendingsTable
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Lending(id: EntityID<Int>) : BookingEntity<ItemLendingD>(id) {
    companion object : IntEntityClass<Lending>(LendingsTable)

    val createdAt by LendingsTable.createdAt

    override var user by User referencedOn LendingsTable.user

    override var confirmed by LendingsTable.confirmed

    override var from by LendingsTable.from
    override var to by LendingsTable.to

    override var takenAt by LendingsTable.takenAt
    override var returnedAt by LendingsTable.returnedAt

    override fun serializable() = ItemLendingD(
        id = id.value,
        createdAt = createdAt.toKotlinInstant(),
        userId = user.id.value,
        confirmed = confirmed,
        from = from.toKotlinLocalDate(),
        to = to.toKotlinLocalDate(),
        takenAt = takenAt?.toKotlinInstant(),
        returnedAt = returnedAt?.toKotlinInstant(),
        itemIds = LendingItem
            .find { LendingItemsTable.lending eq id }
            .map { it.item.id.value }
            .toSet()
    )
}
