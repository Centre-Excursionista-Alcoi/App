package org.centrexcursionistalcoi.app.platform

import io.ktor.http.*
import org.centrexcursionistalcoi.app.storage.fs.AppFile
import org.centrexcursionistalcoi.app.storage.fs.toJavaFile
import org.koin.core.annotation.Singleton
import java.awt.Desktop

@Singleton
actual class PlatformOpenFileLogic : PlatformProvider {
    actual override val isSupported: Boolean = Desktop.isDesktopSupported()

    actual fun open(file: AppFile, contentType: ContentType) {
        Desktop.getDesktop().open(file.toJavaFile())
    }
}
