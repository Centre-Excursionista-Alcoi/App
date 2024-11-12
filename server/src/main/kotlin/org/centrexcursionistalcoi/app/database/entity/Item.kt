package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.database.table.ItemsTable
import org.centrexcursionistalcoi.app.server.response.data.ItemD
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Item(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Item>(ItemsTable)

    val createdAt by ItemsTable.createdAt

    var health by ItemsTable.health

    var type by ItemType referencedOn ItemsTable.type

    fun serializable(): ItemD {
        return ItemD(
            id = id.value,
            createdAt = createdAt.toEpochMilli(),
            health = health,
            typeId = type.id.value
        )
    }
}
