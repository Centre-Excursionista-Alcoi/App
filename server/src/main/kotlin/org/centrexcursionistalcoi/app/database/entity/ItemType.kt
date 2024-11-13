package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.database.table.ItemTypesTable
import org.centrexcursionistalcoi.app.server.response.data.ItemTypeD
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class ItemType(id: EntityID<Int>): IntEntity(id) {
    companion object : IntEntityClass<ItemType>(ItemTypesTable)

    val createdAt by ItemTypesTable.createdAt

    var title by ItemTypesTable.title
    var description by ItemTypesTable.description

    var brand by ItemTypesTable.brand
    var model by ItemTypesTable.model

    var section by Section referencedOn ItemTypesTable.section

    fun serializable(): ItemTypeD = ItemTypeD(
        id = id.value,
        createdAt = createdAt.toEpochMilli(),
        title = title,
        description = description,
        brand = brand,
        model = model,
        sectionId = section.id.value
    )
}
