package org.centrexcursionistalcoi.app.platform

import io.ktor.http.ContentType

actual object PlatformShareLogic {
    actual val sharingSupported: Boolean = false

    actual fun share(path: String, contentType: ContentType) {
        throw UnsupportedOperationException("Sharing is not supported on this platform")
    }
}
