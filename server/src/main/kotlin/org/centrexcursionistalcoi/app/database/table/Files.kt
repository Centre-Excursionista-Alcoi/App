package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.database.DatabaseNowExpression
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.security.FileReadWriteRules
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.json

/**
 * The metadata of stored files. Their contents are in [org.centrexcursionistalcoi.app.storage.FileStorage], at
 * [objectKey].
 */
object Files : UuidTable("files") {
    /** The key of the file's contents in the storage. Never sent to clients. */
    val objectKey = varchar("objectKey", 255).uniqueIndex()
    /** The size of the file's contents, in bytes. */
    val size = long("size")
    val type = varchar("type", 255).nullable()
    val name = varchar("name", 255).nullable()

    val lastModified = timestamp("lastModified").defaultExpression(DatabaseNowExpression)

    val rules = json("rules", json, FileReadWriteRules.serializer()).nullable()
}
