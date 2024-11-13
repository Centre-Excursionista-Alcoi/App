package org.centrexcursionistalcoi.app.database.entity

import java.time.ZoneId
import java.time.ZoneOffset
import org.centrexcursionistalcoi.app.database.table.LendingsTable
import org.centrexcursionistalcoi.app.server.response.data.LendingD
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Lending(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Lending>(LendingsTable)

    val createdAt by LendingsTable.createdAt

    var item by Item referencedOn LendingsTable.item
    var user by User referencedOn LendingsTable.user

    var confirmed by LendingsTable.confirmed

    var from by LendingsTable.from
    var to by LendingsTable.to

    var takenAt by LendingsTable.takenAt
    var returnedAt by LendingsTable.returnedAt

    fun serializable() = LendingD(
        id = id.value,
        createdAt = createdAt.toEpochMilli(),
        itemId = item.id.value,
        userId = user.id.value,
        confirmed = confirmed,
        from = from.toInstant(ZoneOffset.of(ZoneId.systemDefault().id)).toEpochMilli(),
        to = to.toInstant(ZoneOffset.of(ZoneId.systemDefault().id)).toEpochMilli(),
        takenAt = takenAt?.toEpochMilli(),
        returnedAt = returnedAt?.toEpochMilli()
    )
}
