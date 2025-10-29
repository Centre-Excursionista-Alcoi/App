package org.centrexcursionistalcoi.app.platform

import io.ktor.http.ContentType

expect object PlatformPrinter : PlatformProvider {
    override val isSupported: Boolean

    fun printImage(imageData: ByteArray, contentType: ContentType = ContentType.Image.PNG, jobName: String = "Print Job")
}
