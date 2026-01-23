package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.database.DatabaseNowExpression
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.security.FileReadWriteRules
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.json.json

object Files : UUIDTable("files") {
    val bytes = binary("bytes")
    val type = varchar("type", 255).nullable()
    val name = varchar("name", 255).nullable()

    val lastModified = timestamp("lastModified").defaultExpression(DatabaseNowExpression)

    val rules = json("rules", json, FileReadWriteRules.serializer()).nullable()
}
