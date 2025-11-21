package org.centrexcursionistalcoi.app.database.entity

import io.ktor.http.ContentType
import java.util.UUID
import kotlin.time.toKotlinInstant
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.database.table.Files
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

class FileEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<FileEntity>(Files) {
        context(_: JdbcTransaction)
        fun from(withContext: FileWithContext) = new(withContext.id?.toJavaUuid() ?: UUID.randomUUID()) {
            data = withContext.bytes
            type = withContext.contentType?.toString()
            name = withContext.name
        }
    }

    var data by Files.data
    var type by Files.type
    var name by Files.name

    var lastModified by Files.lastModified

    var rules by Files.rules

    /**
     * The content type of the file. Defaults to `application/octet-stream` if not set.
     */
    var contentType: ContentType
        get() = type?.let(ContentType::parse) ?: ContentType.Application.OctetStream
        set(value) { type = value.toString() }

    fun toData(): FileWithContext = FileWithContext(
        id = id.value.toKotlinUuid(),
        name = name,
        bytes = data,
        contentType = contentType,
        lastModified = lastModified.toKotlinInstant(),
    )
}
