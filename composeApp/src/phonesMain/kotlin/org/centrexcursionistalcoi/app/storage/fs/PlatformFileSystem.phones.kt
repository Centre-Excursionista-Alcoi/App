package org.centrexcursionistalcoi.app.storage.fs

import kotlinx.io.Buffer
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

actual object PlatformFileSystem {
    private val fs = SystemFileSystem

    actual suspend fun write(path: String, data: ByteArray) {
        fs.sink(Path(path)).use { sink ->
            val buffer = Buffer()
            buffer.write(data)
            buffer.transferTo(sink.buffered())
        }
    }

    actual suspend fun read(path: String): ByteArray {
        return fs.source(Path(path)).use { source ->
            source.buffered().readByteArray()
        }
    }
}
