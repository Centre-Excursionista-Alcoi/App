package org.centrexcursionistalcoi.app.platform

import io.ktor.http.ContentType
import org.centrexcursionistalcoi.app.storage.fs.AppFile
import org.koin.core.annotation.Singleton

@Singleton
expect class PlatformOpenFileLogic : PlatformProvider {
    override val isSupported: Boolean

    fun open(file: AppFile, contentType: ContentType)
}
