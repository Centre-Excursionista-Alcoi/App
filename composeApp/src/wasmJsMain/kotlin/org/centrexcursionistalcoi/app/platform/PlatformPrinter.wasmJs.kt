package org.centrexcursionistalcoi.app.platform

import io.ktor.http.ContentType
import kotlin.io.encoding.Base64
import kotlinx.browser.window

actual object PlatformPrinter : PlatformProvider {
    actual override val isSupported: Boolean = true

    actual fun printImage(imageData: ByteArray, contentType: ContentType, jobName: String) {
        val imageBase64 = Base64.UrlSafe.encode(imageData)
        val html = """
            <html>
            <body>
            <img src="data:${contentType};base64,${imageBase64}" />
            <script>
                window.onload = function() {
                    window.print();
                }
            </script>
            </body>
            </html>
        """.trimIndent()
        window.open("about:blank", "_new")?.apply {
            document.title = jobName
            document.write(html)
            document.close()
        } ?: error("Error creating document")
    }
}
