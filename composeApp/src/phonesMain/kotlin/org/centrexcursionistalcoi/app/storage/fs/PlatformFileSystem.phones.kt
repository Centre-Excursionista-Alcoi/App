package org.centrexcursionistalcoi.app.storage.fs

import io.github.vinceglb.filekit.utils.div
import io.ktor.util.cio.use
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.asByteWriteChannel
import io.ktor.utils.io.copyTo
import kotlinx.io.Buffer
import kotlinx.io.buffered
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

actual object PlatformFileSystem {
    private val fs = SystemFileSystem

    actual suspend fun write(path: String, data: ByteArray) {
        val path = SystemDataPath / path
        path.parent?.let { fs.createDirectories(it) }
        fs.sink(path).use { sink ->
            val buffer = Buffer()
            buffer.write(data)
            buffer.transferTo(sink.buffered())
        }
    }

    actual suspend fun write(path: String, channel: ByteReadChannel) {
        val path = SystemDataPath / path
        path.parent?.let { fs.createDirectories(it) }
        fs.sink(path).use { sink ->
            sink.asByteWriteChannel().use {
                channel.copyTo(this)
            }
        }
    }

    actual suspend fun read(path: String): ByteArray {
        return fs.source(SystemDataPath / path).use { source ->
            source.buffered().readByteArray()
        }
    }
}
