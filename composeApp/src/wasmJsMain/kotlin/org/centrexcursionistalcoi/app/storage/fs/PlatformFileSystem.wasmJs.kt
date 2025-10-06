package org.centrexcursionistalcoi.app.storage.fs

import kotlinx.coroutines.await

@OptIn(ExperimentalWasmJsInterop::class, ExperimentalUnsignedTypes::class)
actual object PlatformFileSystem {
    private suspend fun getDirectoryHandle(path: String): FileSystemDirectoryHandle {
        require(!path.endsWith("/")) { "Paths must not end with \"/\"" }
        val cleanPath = path.replace("\\", "/")
        val root = OPFS.root()
        // If the path doesn't contain '/', it's in the root directory
        if (!cleanPath.contains('/')) return root
        val parts = cleanPath.split('/')
        var currentDir = root
        for (i in 0 until parts.size - 1) {
            val part = parts[i]
            currentDir = currentDir.getDirectoryHandle(part, createFSFHGetOptions(true)).await()
        }
        return currentDir
    }

    actual suspend fun write(path: String, data: ByteArray) {
        val dir = getDirectoryHandle(path)
        OPFS.writeFile(dir, path.split('/').last(), data)
    }

    actual suspend fun read(path: String): ByteArray {
        val dir = getDirectoryHandle(path)
        val contents = OPFS.readFile(dir, path.split('/').last())
        requireNotNull(contents) { "File $path not found" }
        return contents
    }
}
