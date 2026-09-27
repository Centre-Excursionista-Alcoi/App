package org.centrexcursionistalcoi.app.storage

import java.io.InputStream
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.absolute
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isRegularFile
import kotlin.io.path.isWritable
import kotlin.io.path.outputStream
import kotlin.io.path.relativeTo

/**
 * Stores objects as files in a local directory. Only meant for development, when no S3 storage is configured.
 */
class LocalFileStorage(root: Path) : FileStorage {
    private val root: Path = root.absolute().normalize()

    override val description: String = "local directory ${this.root}"

    private val validKey = Regex("^[A-Za-z0-9._-]+(/[A-Za-z0-9._-]+)*$")

    private fun resolve(key: String): Path {
        require(validKey.matches(key) && key.split('/').none { it == "." || it == ".." }) { "Invalid object key: $key" }
        return root.resolve(key).normalize().also {
            require(it.startsWith(root)) { "Invalid object key: $key" }
        }
    }

    private fun write(key: String, writer: (Path) -> Unit) {
        val path = resolve(key)
        path.parent.createDirectories()
        val temp = Files.createTempFile(path.parent, ".upload-", ".tmp")
        try {
            writer(temp)
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } finally {
            temp.deleteIfExists()
        }
    }

    override fun put(key: String, bytes: ByteArray, contentType: String) {
        write(key) { Files.write(it, bytes) }
    }

    override fun put(key: String, input: InputStream, size: Long, contentType: String) {
        write(key) { temp ->
            val copied = temp.outputStream().use { input.copyTo(it) }
            check(copied == size) { "Expected $size bytes for $key, got $copied" }
        }
    }

    override fun open(key: String): InputStream = try {
        resolve(key).inputStream()
    } catch (e: NoSuchFileException) {
        throw StoredObjectNotFoundException(key, e)
    }

    override fun head(key: String): StoredObjectInfo? =
        resolve(key).takeIf { it.isRegularFile() }?.let { StoredObjectInfo(size = it.fileSize()) }

    override fun delete(key: String) {
        resolve(key).deleteIfExists()
    }

    override fun keys(prefix: String): List<String> {
        if (!Files.isDirectory(root)) return emptyList()
        return Files.walk(root).use { paths ->
            paths.filter { it.isRegularFile() && !it.fileName.toString().startsWith(".upload-") }
                .map { it.relativeTo(root).invariantSeparatorsPathString }
                .filter { it.startsWith(prefix) }
                .toList()
        }
    }

    override fun checkAvailable() {
        try {
            root.createDirectories()
        } catch (e: Exception) {
            throw IllegalStateException("Cannot create the files directory $root: ${e.message}", e)
        }
        check(root.isWritable()) { "The files directory $root is not writable" }
    }
}
