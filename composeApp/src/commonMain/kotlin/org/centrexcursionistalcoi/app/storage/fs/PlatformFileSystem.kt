package org.centrexcursionistalcoi.app.storage.fs

import io.ktor.utils.io.ByteReadChannel
import org.centrexcursionistalcoi.app.process.ProgressNotifier

expect object PlatformFileSystem {
    suspend fun write(path: String, channel: ByteReadChannel, progress: (ProgressNotifier)? = null)

    suspend fun read(path: String, progress: (ProgressNotifier)? = null): ByteArray

    suspend fun exists(path: String, progress: (ProgressNotifier)? = null): Boolean
}
