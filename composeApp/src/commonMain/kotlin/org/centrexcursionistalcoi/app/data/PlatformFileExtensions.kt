package org.centrexcursionistalcoi.app.data

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import io.ktor.http.ContentType
import io.ktor.http.defaultForFileExtension

suspend fun PlatformFile.fileWithContext(): FileWithContext {
    val bytes = this.readBytes()
    val contentType = ContentType.defaultForFileExtension(extension)
    return FileWithContext(
        bytes = bytes,
        name = name,
        contentType = contentType,
    )
}
