package org.centrexcursionistalcoi.app.storage.fs

import io.github.aakira.napier.Napier
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.toByteArray
import kotlinx.coroutines.await

@OptIn(ExperimentalWasmJsInterop::class, ExperimentalUnsignedTypes::class)
actual object PlatformFileSystem {
    private suspend fun getDirectoryHandle(path: String): FileSystemDirectoryHandle {
        require(!path.endsWith("/")) { "Paths must not end with \"/\"" }
        val cleanPath = path.replace("\\", "/")
        val root = OPFS.root()
        // If the path doesn't contain '/', it's in the root directory
        if (!cleanPath.contains('/')) return root
        val parts = cleanPath.split('/').filter { it.isNotBlank() }
        var currentDir = root
        for (part in (parts - parts.last())) {
            Napier.v { "Getting or creating directory: $part" }
            currentDir = currentDir.getDirectoryHandle(part, createFSFHGetOptions(true)).await()
        }
        return currentDir
    }

    actual suspend fun write(path: String, data: ByteArray) {
        val dir = getDirectoryHandle(path)
        OPFS.writeFile(dir, path.split('/').last(), data)
    }

    actual suspend fun write(path: String, channel: ByteReadChannel) = write(path, channel.toByteArray())

    actual suspend fun read(path: String): ByteArray {
        val dir = getDirectoryHandle(path)
        val contents = OPFS.readFile(dir, path.split('/').last())
        requireNotNull(contents) { "File $path not found" }
        return contents
    }

    actual suspend fun exists(path: String): Boolean {
        val dir = getDirectoryHandle(path)
        return OPFS.exists(dir, path.split('/').last())
    }
}
