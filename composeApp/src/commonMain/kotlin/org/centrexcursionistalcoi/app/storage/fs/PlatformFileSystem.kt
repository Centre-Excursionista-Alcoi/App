package org.centrexcursionistalcoi.app.storage.fs

expect object PlatformFileSystem {
    suspend fun write(path: String, data: ByteArray)

    suspend fun read(path: String): ByteArray
}
