package org.centrexcursionistalcoi.app.push

actual object PlatformSSEConfiguration {
    actual val enableSSE: Boolean = true // FCM doesn't support JVM, so we use SSE
}
