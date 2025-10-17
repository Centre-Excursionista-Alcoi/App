package org.centrexcursionistalcoi.app.platform

expect object PlatformNFC {
    val supportsNFC: Boolean

    suspend fun readNFC(): String?

    suspend fun writeNFC(message: String)
}
