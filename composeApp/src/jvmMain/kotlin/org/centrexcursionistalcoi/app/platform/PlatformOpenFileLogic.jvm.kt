package org.centrexcursionistalcoi.app.platform

import io.github.vinceglb.filekit.utils.div
import io.ktor.http.ContentType
import java.awt.Desktop
import java.io.File
import org.centrexcursionistalcoi.app.storage.fs.SystemDataPath

actual object PlatformOpenFileLogic {
    actual val supported: Boolean
        get() = TODO("Not yet implemented")

    actual fun open(path: String, contentType: ContentType) {
        val filePath = SystemDataPath / path
        val file = File(filePath.toString())

        Desktop.getDesktop().open(file)
    }
}
