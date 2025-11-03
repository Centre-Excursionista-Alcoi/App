package org.centrexcursionistalcoi.app.platform

expect object PlatformNFC : PlatformProvider {
    override val isSupported: Boolean

    suspend fun readNFC(): String?

    suspend fun writeNFC(message: String)
}

val PlatformNFC.isNotSupported: Boolean
    get() = !isSupported
