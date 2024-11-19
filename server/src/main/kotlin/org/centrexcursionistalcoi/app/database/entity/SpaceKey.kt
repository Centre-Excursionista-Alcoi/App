package org.centrexcursionistalcoi.app.database.entity

import org.centrexcursionistalcoi.app.data.SpaceKeyD
import org.centrexcursionistalcoi.app.database.common.SerializableEntity
import org.centrexcursionistalcoi.app.database.table.SpaceKeysTable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class SpaceKey(id: EntityID<Int>) : IntEntity(id), SerializableEntity<SpaceKeyD> {
    companion object : IntEntityClass<SpaceKey>(SpaceKeysTable)

    val createdAt by SpaceKeysTable.createdAt

    var name by SpaceKeysTable.name
    var description by SpaceKeysTable.description

    var space by Space referencedOn SpaceKeysTable.space

    override fun serializable(): SpaceKeyD = SpaceKeyD(
        id = id.value,
        createdAt = createdAt.toEpochMilli(),
        name = name,
        description = description,
        spaceId = space.id.value
    )
}
