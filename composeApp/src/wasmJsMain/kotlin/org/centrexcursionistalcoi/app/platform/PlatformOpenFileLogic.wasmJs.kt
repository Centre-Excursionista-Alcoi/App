package org.centrexcursionistalcoi.app.platform

import io.ktor.http.ContentType

actual object PlatformOpenFileLogic {
    actual val supported: Boolean = false

    actual fun open(path: String, contentType: ContentType) {
        throw UnsupportedOperationException("Opening files is not supported on web.")
    }
}
