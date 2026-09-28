package org.centrexcursionistalcoi.app.database.table

import kotlin.uuid.Uuid
import kotlinx.serialization.SerializationStrategy
import org.centrexcursionistalcoi.app.database.DatabaseNowExpression
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.database.utils.CustomTableSerializer
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.security.FileReadWriteRules
import org.centrexcursionistalcoi.app.security.UserSession
import org.centrexcursionistalcoi.app.serializer.Base64Serializer
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.json.json

/**
 * The metadata of stored files. Their contents are in [org.centrexcursionistalcoi.app.storage.FileStorage], at
 * [objectKey].
 */
object Files : UuidTable("files"), CustomTableSerializer<Uuid, FileEntity> {
    /** The key of the file's contents in the storage. Never sent to clients. */
    val objectKey = varchar("objectKey", 255).uniqueIndex()
    /** The size of the file's contents, in bytes. */
    val size = long("size")
    val type = varchar("type", 255).nullable()
    val name = varchar("name", 255).nullable()

    val lastModified = timestamp("lastModified").defaultExpression(DatabaseNowExpression)

    val rules = json("rules", json, FileReadWriteRules.serializer()).nullable()

    // Files used to be serialized (as post files) with their contents in "bytes", which clients require to be
    // present. Contents are downloaded from /download/{id}, so it's always empty now.
    override fun columnSerializers(): Map<String, SerializationStrategy<*>> = mapOf("bytes" to Base64Serializer)

    context(_: JdbcTransaction)
    override fun extraColumns(entity: FileEntity, session: UserSession?): Map<String, Any?> = mapOf("bytes" to ByteArray(0))
}
