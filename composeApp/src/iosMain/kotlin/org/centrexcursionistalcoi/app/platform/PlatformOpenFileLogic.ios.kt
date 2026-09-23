package org.centrexcursionistalcoi.app.platform

import io.ktor.http.ContentType
import org.centrexcursionistalcoi.app.storage.fs.AppFile
import org.koin.core.annotation.Singleton

@Singleton
actual class PlatformOpenFileLogic : PlatformProvider {
    actual override val isSupported: Boolean = false

    actual fun open(file: AppFile, contentType: ContentType) {
        throw UnsupportedOperationException("Opening files is not supported on iOS.")
    }
}
