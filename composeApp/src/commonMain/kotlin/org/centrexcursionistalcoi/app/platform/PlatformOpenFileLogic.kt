package org.centrexcursionistalcoi.app.platform

import io.ktor.http.ContentType

expect object PlatformOpenFileLogic {
    val supported: Boolean

    fun open(path: String, contentType: ContentType)
}
