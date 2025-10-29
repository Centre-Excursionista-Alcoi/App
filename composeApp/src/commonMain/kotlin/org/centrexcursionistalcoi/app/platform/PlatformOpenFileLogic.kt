package org.centrexcursionistalcoi.app.platform

import io.ktor.http.ContentType

expect object PlatformOpenFileLogic : PlatformProvider {
    override val isSupported: Boolean

    fun open(path: String, contentType: ContentType)
}
