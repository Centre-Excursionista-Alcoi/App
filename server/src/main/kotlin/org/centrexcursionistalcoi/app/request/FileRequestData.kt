package org.centrexcursionistalcoi.app.request

import io.ktor.http.ContentType
import io.ktor.http.content.PartData
import io.ktor.utils.io.core.Closeable
import io.ktor.utils.io.jvm.javaio.copyTo
import java.io.ByteArrayOutputStream
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.FileEntity

class FileRequestData: Closeable {
    var contentType: ContentType? = null
    var originalFileName: String? = null
    val baos: ByteArrayOutputStream = ByteArrayOutputStream()

    fun isEmpty(): Boolean = baos.size() <= 0

    fun isNotEmpty(): Boolean = !isEmpty()

    suspend fun populate(partData: PartData.FileItem) {
        contentType = partData.contentType
        originalFileName = partData.originalFileName
        partData.provider().copyTo(baos)
    }

    /**
     * Creates a new [FileEntity] in the database with the data from this file and releases resources.
     * @return The created [FileEntity].
     */
    fun newEntity(): FileEntity {
        return Database {
            FileEntity.new {
                name = originalFileName ?: "unknown"
                type = contentType?.toString() ?: "application/octet-stream"
                data = baos.toByteArray()
            }
        }.also { close() }
    }

    /**
     * Closes this file data and releases resources.
     */
    override fun close() {
        baos.flush()
        baos.close()
    }
}
