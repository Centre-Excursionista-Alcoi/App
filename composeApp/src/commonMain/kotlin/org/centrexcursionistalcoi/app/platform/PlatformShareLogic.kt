package org.centrexcursionistalcoi.app.platform

import io.ktor.http.ContentType

expect object PlatformShareLogic {
    val sharingSupported: Boolean

    fun share(data: ByteArray, contentType: ContentType)
}
