package org.centrexcursionistalcoi.app.platform

import io.ktor.http.ContentType
import org.koin.core.annotation.Singleton

@Singleton
expect class PlatformShareLogic : PlatformProvider {
    override val isSupported: Boolean

    fun share(path: String, contentType: ContentType)

    fun share(text: String)
}
