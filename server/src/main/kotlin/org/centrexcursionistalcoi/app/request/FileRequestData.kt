package org.centrexcursionistalcoi.app.request

import io.ktor.http.ContentType
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.copyTo
import java.io.ByteArrayOutputStream
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.FileEntity

class FileRequestData {
    var contentType: ContentType? = null
    var originalFileName: String? = null
    var byteReadChannel: ByteReadChannel? = null

    fun isEmpty(): Boolean = byteReadChannel == null

    fun isNotEmpty(): Boolean = !isEmpty()

    suspend fun newEntity(): FileEntity {
        val channel = byteReadChannel ?: throw NullPointerException("FileRequestData cannot have null channel")
        val bytes = ByteArrayOutputStream()
        channel.copyTo(bytes)

        return Database {
            FileEntity.new {
                name = originalFileName ?: "unknown"
                type = contentType?.toString() ?: "application/octet-stream"
                data = bytes.toByteArray()
            }
        }
    }
}
