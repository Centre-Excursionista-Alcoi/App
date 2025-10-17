package org.centrexcursionistalcoi.app.storage.fs

import io.ktor.utils.io.ByteReadChannel

expect object PlatformFileSystem {
    suspend fun write(path: String, data: ByteArray)

    suspend fun write(path: String, channel: ByteReadChannel)

    suspend fun read(path: String): ByteArray
}
