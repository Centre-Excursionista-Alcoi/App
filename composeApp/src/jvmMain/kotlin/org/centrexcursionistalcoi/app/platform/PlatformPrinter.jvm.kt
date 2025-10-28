package org.centrexcursionistalcoi.app.platform

import io.ktor.http.ContentType

actual object PlatformPrinter {
    actual val supportsPrinting: Boolean = false

    actual fun printImage(imageData: ByteArray, contentType: ContentType, jobName: String) {
        // TODO: Implement printing functionality for JVM platform
    }
}
