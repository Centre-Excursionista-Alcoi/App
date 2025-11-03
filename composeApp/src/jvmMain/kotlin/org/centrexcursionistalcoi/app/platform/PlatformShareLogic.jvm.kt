package org.centrexcursionistalcoi.app.platform

import io.ktor.http.ContentType

actual object PlatformShareLogic : PlatformProvider {
    actual override val isSupported: Boolean = false

    actual fun share(path: String, contentType: ContentType) {
        throw UnsupportedOperationException("Sharing is not supported on JVM platform.")
    }

    actual fun share(text: String) {
        throw UnsupportedOperationException("Sharing is not supported on this platform")
    }
}
