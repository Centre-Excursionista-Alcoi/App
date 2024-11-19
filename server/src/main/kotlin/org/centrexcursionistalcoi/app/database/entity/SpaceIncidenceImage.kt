package org.centrexcursionistalcoi.app.database.entity

import java.util.UUID
import org.centrexcursionistalcoi.app.database.table.SpaceIncidenceImagesTable
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class SpaceIncidenceImage(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<SpaceIncidenceImage>(SpaceIncidenceImagesTable)

    val createdAt by SpaceIncidenceImagesTable.createdAt

    var image by SpaceIncidenceImagesTable.image

    var incidence by SpaceIncidence referencedOn SpaceIncidenceImagesTable.incidence
}
