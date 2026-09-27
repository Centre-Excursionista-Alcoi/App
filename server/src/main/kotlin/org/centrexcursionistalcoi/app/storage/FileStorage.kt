package org.centrexcursionistalcoi.app.storage

import java.io.Closeable
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream

/**
 * Where the contents of files ([org.centrexcursionistalcoi.app.database.entity.FileEntity]) are stored. The database
 * only keeps each file's metadata and the key of its object here.
 *
 * Keys are opaque, `/`-separated strings (e.g. `files/<uuid>`). Every operation is blocking.
 */
interface FileStorage : Closeable {
    /**
     * A human-readable description of where objects are stored, for logs.
     */
    val description: String

    /**
     * Stores [bytes] at [key], replacing any existing object.
     */
    fun put(key: String, bytes: ByteArray, contentType: String)

    /**
     * Stores [size] bytes read from [input] at [key], replacing any existing object. [input] is not closed.
     */
    fun put(key: String, input: InputStream, size: Long, contentType: String)

    /**
     * Stores the contents of [file] at [key], replacing any existing object. Streamed, never loaded into memory.
     */
    fun put(key: String, file: Path, contentType: String) {
        file.inputStream().use { put(key, it, file.fileSize(), contentType) }
    }

    /**
     * Opens the object at [key] for reading. The caller must close the stream.
     * @throws StoredObjectNotFoundException if there's no object at [key].
     */
    fun open(key: String): InputStream

    /**
     * Reads the whole object at [key] into memory.
     * @throws StoredObjectNotFoundException if there's no object at [key].
     */
    fun readBytes(key: String): ByteArray = open(key).use { it.readBytes() }

    /**
     * Gets the information of the object at [key], or `null` if there's none.
     */
    fun head(key: String): StoredObjectInfo?

    /**
     * Deletes the object at [key]. Does nothing if there's none.
     */
    fun delete(key: String)

    /**
     * Lists the keys of every stored object starting with [prefix].
     */
    fun keys(prefix: String = ""): List<String>

    /**
     * Checks that the storage can be used.
     * @throws IllegalStateException with a description of the problem if it can't.
     */
    fun checkAvailable()

    override fun close() {}
}

data class StoredObjectInfo(val size: Long)

class StoredObjectNotFoundException(key: String, cause: Throwable? = null) :
    NoSuchElementException("Object not found in storage: $key") {
    init {
        cause?.let(::initCause)
    }
}
