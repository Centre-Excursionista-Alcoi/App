package org.centrexcursionistalcoi.app.storage.fs

import io.github.vinceglb.filekit.utils.div
import io.ktor.util.cio.use
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.asByteWriteChannel
import io.ktor.utils.io.copyTo
import kotlinx.io.buffered
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.utils.copyTo

actual object PlatformFileSystem {
    private val fs = SystemFileSystem

    actual suspend fun write(path: String, channel: ByteReadChannel, progress: (ProgressNotifier)?) {
        val path = SystemDataPath / path
        path.parent?.let { fs.createDirectories(it) }
        fs.sink(path).use { sink ->
            sink.asByteWriteChannel().use {
                if (progress != null) channel.copyTo(this, progress)
                else channel.copyTo(this)
            }
        }
    }

    actual suspend fun read(path: String, progress: (ProgressNotifier)?): ByteArray {
        return fs.source(SystemDataPath / path).use { source ->
            source.buffered().readByteArray()
        }
    }

    actual suspend fun exists(path: String, progress: (ProgressNotifier)?): Boolean {
        return fs.exists(SystemDataPath / path)
    }
}
