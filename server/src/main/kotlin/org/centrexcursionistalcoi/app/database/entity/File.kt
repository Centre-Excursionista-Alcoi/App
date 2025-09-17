package org.centrexcursionistalcoi.app.database.entity

import java.util.UUID
import org.centrexcursionistalcoi.app.database.table.Files
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass

class File(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<File>(Files)

    var data by Files.data
    var type by Files.type
    var name by Files.name

    var rules by Files.rules
}
