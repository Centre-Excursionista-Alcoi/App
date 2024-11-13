package org.centrexcursionistalcoi.app.database.entity

import java.time.ZoneId
import org.centrexcursionistalcoi.app.database.table.LendingItemsTable
import org.centrexcursionistalcoi.app.database.table.LendingsTable
import org.centrexcursionistalcoi.app.server.response.data.LendingD
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Lending(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Lending>(LendingsTable)

    val createdAt by LendingsTable.createdAt

    var user by User referencedOn LendingsTable.user

    var confirmed by LendingsTable.confirmed

    var from by LendingsTable.from
    var to by LendingsTable.to

    var takenAt by LendingsTable.takenAt
    var returnedAt by LendingsTable.returnedAt

    fun serializable() = LendingD(
        id = id.value,
        createdAt = createdAt.toEpochMilli(),
        userId = user.id.value,
        confirmed = confirmed,
        from = from.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        to = to.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        takenAt = takenAt?.toEpochMilli(),
        returnedAt = returnedAt?.toEpochMilli(),
        itemIds = LendingItem
            .find { LendingItemsTable.lending eq id }
            .map { it.item.id.value }
            .toSet()
    )
}
