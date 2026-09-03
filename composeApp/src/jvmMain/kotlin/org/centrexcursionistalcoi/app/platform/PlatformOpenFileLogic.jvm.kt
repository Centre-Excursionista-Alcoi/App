package org.centrexcursionistalcoi.app.platform

import io.github.vinceglb.filekit.utils.div
import io.ktor.http.ContentType
import org.centrexcursionistalcoi.app.storage.fs.SystemDataPath
import org.koin.core.annotation.Singleton
import java.awt.Desktop
import java.io.File

@Singleton
actual class PlatformOpenFileLogic : PlatformProvider {
    actual override val isSupported: Boolean = Desktop.isDesktopSupported()

    actual fun open(path: String, contentType: ContentType) {
        val filePath = SystemDataPath / path
        val file = File(filePath.toString())

        Desktop.getDesktop().open(file)
    }
}
