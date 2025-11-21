package org.centrexcursionistalcoi.app.request

import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData
import io.ktor.utils.io.core.Closeable
import io.ktor.utils.io.jvm.javaio.copyTo
import java.io.ByteArrayOutputStream
import kotlin.io.encoding.Base64
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.FileEntity

class FileRequestData : Closeable {
    companion object {
        /**
         * Converts a [FileWithContext] to a [FileRequestData].
         */
        fun FileWithContext.toFileRequestData() = FileRequestData().apply {
            this.contentType = this@toFileRequestData.contentType
            this.originalFileName = this@toFileRequestData.name
            this.baos.writeBytes(this@toFileRequestData.bytes)
        }
    }

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
     * Populates this file data from the given [PartData.FormItem].
     *
     * Data will be provided as a Base64-encoded string in the form item.
     */
    fun populate(partData: PartData.FormItem) {
        contentType = partData.contentType

        val filename = partData.headers[HttpHeaders.ContentDisposition]
            ?.let(ContentDisposition::parse)
            ?.parameters
            ?.find { it.name.equals("filename", true) }
            ?.value
        originalFileName = filename

        val value = Base64.UrlSafe.decode(partData.value)
        baos.writeBytes(value)
    }

    /**
     * Creates a new [FileEntity] in the database with the data from this file and releases resources.
     * @param close Whether to close this file data after creating the entity. Defaults to true.
     * @return The created [FileEntity].
     */
    fun newEntity(close: Boolean = true): FileEntity {
        return Database {
            FileEntity.new {
                this.name = originalFileName ?: "unknown"
                this.contentType = this@FileRequestData.contentType ?: ContentType.Application.OctetStream
                this.bytes = baos.toByteArray()
            }
        }.also { if (close) close() }
    }

    /**
     * Closes this file data and releases resources.
     */
    override fun close() {
        baos.flush()
        baos.close()
    }
}
