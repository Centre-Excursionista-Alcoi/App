package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.database.table.LendingItemsTable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class LendingItem(id: EntityID<Int>): IntEntity(id) {
    companion object : IntEntityClass<LendingItem>(LendingItemsTable)

    var item by Item referencedOn LendingItemsTable.item
    var lending by Lending referencedOn LendingItemsTable.lending
}
