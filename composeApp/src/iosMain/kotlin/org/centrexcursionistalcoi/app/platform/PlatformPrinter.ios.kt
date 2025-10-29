package org.centrexcursionistalcoi.app.platform

import io.ktor.http.ContentType

actual object PlatformPrinter : PlatformProvider {
    actual override val isSupported: Boolean = false

    actual fun printImage(imageData: ByteArray, contentType: ContentType, jobName: String) {
        throw NotImplementedError("Printing is not supported on this platform")
    }
}
