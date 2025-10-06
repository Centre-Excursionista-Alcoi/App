package org.centrexcursionistalcoi.app.request

import io.ktor.http.ContentType
import java.io.ByteArrayOutputStream
import org.centrexcursionistalcoi.app.database.entity.FileEntity

class FileRequestData {
    var contentType: ContentType? = null
    var originalFileName: String? = null
    val dataStream = ByteArrayOutputStream()

    fun isEmpty(): Boolean = dataStream.size() <= 0

    fun isNotEmpty(): Boolean = !isEmpty()

    fun newEntity() = FileEntity.new {
        name = originalFileName ?: "unknown"
        type = contentType?.toString() ?: "application/octet-stream"
        data = dataStream.toByteArray()
    }
}
