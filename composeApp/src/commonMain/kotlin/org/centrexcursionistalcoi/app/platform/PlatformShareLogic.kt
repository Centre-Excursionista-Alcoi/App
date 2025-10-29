package org.centrexcursionistalcoi.app.platform

import io.ktor.http.ContentType

expect object PlatformShareLogic : PlatformProvider {
    override val isSupported: Boolean

    fun share(path: String, contentType: ContentType)
}
