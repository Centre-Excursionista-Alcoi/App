package org.centrexcursionistalcoi.app.platform

import io.ktor.http.ContentType

actual object PlatformOpenFileLogic : PlatformProvider {
    actual override val isSupported: Boolean = false

    actual fun open(path: String, contentType: ContentType) {
        throw UnsupportedOperationException("Opening files is not supported on iOS.")
    }
}
