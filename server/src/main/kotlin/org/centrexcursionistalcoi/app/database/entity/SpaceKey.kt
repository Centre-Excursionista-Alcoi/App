package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.database.table.SpaceKeysTable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class SpaceKey(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<SpaceKey>(SpaceKeysTable)

    val createdAt by SpaceKeysTable.createdAt

    var name by SpaceKeysTable.name
    var description by SpaceKeysTable.description

    var space by Space referencedOn SpaceKeysTable.space
}
