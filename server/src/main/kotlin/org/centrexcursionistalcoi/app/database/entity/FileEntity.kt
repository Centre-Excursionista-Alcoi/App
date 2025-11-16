package org.centrexcursionistalcoi.app.database.entity

import java.util.UUID
import org.centrexcursionistalcoi.app.database.table.Files
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass

class FileEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<FileEntity>(Files)

    var data by Files.data
    var type by Files.type
    var name by Files.name

    var lastModified by Files.lastModified

    var rules by Files.rules
}
