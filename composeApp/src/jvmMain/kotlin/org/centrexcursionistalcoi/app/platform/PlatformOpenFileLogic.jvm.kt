package org.centrexcursionistalcoi.app.platform

import io.github.vinceglb.filekit.utils.div
import io.ktor.http.*
import org.centrexcursionistalcoi.app.di.PathsProvider
import org.koin.core.annotation.Singleton
import java.awt.Desktop
import java.io.File

@Singleton
actual class PlatformOpenFileLogic(private val pathsProvider: PathsProvider) : PlatformProvider {
    actual override val isSupported: Boolean = Desktop.isDesktopSupported()

    actual fun open(path: String, contentType: ContentType) {
        val filePath = pathsProvider.systemDataPath / path
        val file = File(filePath.toString())

        Desktop.getDesktop().open(file)
    }
}
