package org.centrexcursionistalcoi.app.database.entity.base

import io.ktor.http.ContentType
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

interface ImageContainerEntity {
    var image: FileEntity?

    /**
     * Update the existing image or set a new one if it doesn't exist.
     * If [bytes] is null, the function does nothing.
     */
    @Deprecated(
        message = "Use FileWithContext",
        replaceWith = ReplaceWith(
            "updateOrSetImage(FileWithContext(bytes, name, contentType))",
            "org.centrexcursionistalcoi.app.data.FileWithContext"
        )
    )
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

    /**
     * Update the existing image or set a new one if it doesn't exist.
     * If [file] is null, the function does nothing.
     */
    context(_: JdbcTransaction)
    fun updateOrSetImage(file: FileWithContext?) {
        file ?: return

        val id = file.id
        if (id != null) {
            val image = image
            if (image != null) {
                if (image.id.value.toKotlinUuid() != file.id) {
                    // Different image, delete the old one and create a new one
                    image.delete()
                    this.image = FileEntity.new {
                        this.name = file.name ?: "${id}_file"
                        this.contentType = file.contentType ?: ContentType.Application.OctetStream
                        this.data = file.bytes
                    }
                } else {
                    // Same image, just update the data
                    image.name = file.name ?: image.name
                    image.contentType = file.contentType ?: ContentType.Application.OctetStream
                    image.data = file.bytes
                }
            } else {
                // No existing image, create a new one
                this.image = FileEntity.new(id.toJavaUuid()) {
                    this.name = file.name ?: "${id}_file"
                    this.contentType = file.contentType ?: ContentType.Application.OctetStream
                    this.data = file.bytes
                }
            }
        } else {
            if (image != null) image?.delete()

            image = FileEntity.new {
                this.name = file.name ?: "${this.id}_file"
                this.contentType = file.contentType ?: ContentType.Application.OctetStream
                this.data = file.bytes
            }
        }
    }
}
