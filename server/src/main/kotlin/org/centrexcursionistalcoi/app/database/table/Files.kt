package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.plugins.json
import org.centrexcursionistalcoi.app.security.FileReadWriteRules
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.json.json

object Files : UUIDTable("files") {
    val data = binary("data")
    val type = varchar("type", 255).nullable()
    val name = varchar("name", 255).nullable()

    val rules = json("rules", json, FileReadWriteRules.serializer()).nullable()
}
