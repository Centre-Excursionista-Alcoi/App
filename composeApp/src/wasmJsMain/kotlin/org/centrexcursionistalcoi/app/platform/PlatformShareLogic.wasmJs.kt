package org.centrexcursionistalcoi.app.platform

import io.ktor.http.ContentType

actual object PlatformShareLogic {
    actual val sharingSupported: Boolean = false

    actual fun share(data: ByteArray, contentType: ContentType) {
        throw UnsupportedOperationException("Sharing is not supported on this platform")
    }
}
