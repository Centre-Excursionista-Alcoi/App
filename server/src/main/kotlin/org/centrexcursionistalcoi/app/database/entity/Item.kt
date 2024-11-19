package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.data.ItemD
import org.centrexcursionistalcoi.app.database.common.SerializableEntity
import org.centrexcursionistalcoi.app.database.table.ItemsTable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Item(id: EntityID<Int>) : IntEntity(id), SerializableEntity<ItemD> {
    companion object : IntEntityClass<Item>(ItemsTable)

    val createdAt by ItemsTable.createdAt

    var health by ItemsTable.health
    var notes by ItemsTable.notes

    var type by ItemType referencedOn ItemsTable.type

    override fun serializable(): ItemD {
        return ItemD(
            id = id.value,
            createdAt = createdAt.toEpochMilli(),
            health = health,
            notes = notes,
            typeId = type.id.value
        )
    }
}
