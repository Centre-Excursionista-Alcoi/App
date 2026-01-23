package org.centrexcursionistalcoi.app.database.entity

import io.ktor.http.ContentType
import java.util.UUID
import kotlin.time.toKotlinInstant
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.table.Files
import org.centrexcursionistalcoi.app.utils.detectFileType
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.slf4j.LoggerFactory

class FileEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<FileEntity>(Files) {
        private val logger = LoggerFactory.getLogger(FileEntity::class.java)

        context(_: JdbcTransaction)
        fun newFrom(withContext: FileWithContext) = new(withContext.id?.toJavaUuid() ?: UUID.randomUUID()) {
            bytes = withContext.bytes
            type = withContext.contentType?.toString()
            name = withContext.name
        }

        context(tr: JdbcTransaction)
        fun updateOrCreate(from: FileWithContext, onDelete: JdbcTransaction.(FileEntity) -> Unit = {}): FileEntity? {
            if (from.isEmpty()) {
                // No bytes given, remove existing file
                val fileId = from.id?.toJavaUuid()
                if (fileId != null) {
                    // Remove file
                    findById(fileId)?.let {
                        logger.info("Removing file $fileId from entity...")
                        onDelete(tr, it)
                        it.delete()
                    } ?: run {
                        logger.warn("Asked to remove file $fileId from entity, but file does not exist, ignoring")
                    }
                } else {
                    // Asked to remove, but no id given, ignore
                    logger.warn("Asked to remove file from entity, but no id given, ignoring")
                }
                return null
            } else {
                // Create a new file
                return newFrom(from)
            }
        }
    }

    var bytes by Files.bytes
    var type by Files.type
    var name by Files.name

    var lastModified by Files.lastModified

    var rules by Files.rules

    /**
     * The content type of the file. Defaults to `application/octet-stream` if not set.
     */
    var contentType: ContentType
        get() {
            val type = type?.let(ContentType::parse) ?: ContentType.Application.OctetStream
            if (type == ContentType.Application.OctetStream) {
                return detectFileType(bytes)?.contentType?.also { contentType ->
                    Database { this@FileEntity.type = contentType.toString() }
                } ?: ContentType.Application.OctetStream
            }
            return type
        }
        set(value) { Database { type = value.toString() } }

    fun toData(): FileWithContext = FileWithContext(
        id = id.value.toKotlinUuid(),
        name = name,
        bytes = bytes,
        contentType = contentType,
        lastModified = lastModified.toKotlinInstant(),
    )
}
