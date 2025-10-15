package org.centrexcursionistalcoi.app.database.entity.base

import io.ktor.http.ContentType
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

interface ImageContainerEntity {
    var image: FileEntity?

    /**
     * Update the existing image or set a new one if it doesn't exist.
     * If `bytes` is null, the function does nothing.
     */
    context(_: JdbcTransaction)
    fun updateOrSetImage(bytes: ByteArray?, name: String? = null, contentType: ContentType = ContentType.Application.OctetStream) {
        if (bytes == null) return

        if (image != null) image?.delete()

        image = FileEntity.new {
            this.name = name ?: "${id.value}_file"
            this.type = contentType.toString()
            this.data = bytes
        }
    }
}
