package org.centrexcursionistalcoi.app.database.entity

import java.util.UUID
import org.centrexcursionistalcoi.app.database.table.SpacesImagesTable
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class SpaceImage(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<SpaceImage>(SpacesImagesTable)

    val createdAt by SpacesImagesTable.createdAt

    var image by SpacesImagesTable.image

    var space by Space referencedOn SpacesImagesTable.space
}
