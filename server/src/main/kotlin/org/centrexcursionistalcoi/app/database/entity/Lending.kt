package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.database.table.LendingsTable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Lending(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Lending>(LendingsTable)

    val createdAt by LendingsTable.createdAt

    var item by Item referencedOn LendingsTable.item
    var user by User referencedOn LendingsTable.user

    var from by LendingsTable.from
    var to by LendingsTable.to

    var takenAt by LendingsTable.takenAt
    var returnedAt by LendingsTable.returnedAt
}
