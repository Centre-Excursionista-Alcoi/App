package org.centrexcursionistalcoi.app.database.entity.base

import io.ktor.http.ContentType
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

        val oldImage = image
        image = FileEntity.create(bytes, name, contentType)
        oldImage?.delete()
    }

    /**
     * Update the existing image or set a new one if it doesn't exist.
     * If [file] is null, the function does nothing.
     */
    context(_: JdbcTransaction)
    fun updateOrSetImage(file: FileWithContext?) {
        file ?: return

        val image = image
        if (image != null && file.id != null && image.id.value == file.id) {
            // Same image, just update the data
            image.replaceContents(file)
        } else {
            // New image (keeping the requested id if it's free), then delete the old one
            this.image = FileEntity.newFrom(file)
            image?.delete()
        }
    }
}
