package org.centrexcursionistalcoi.app.storage

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Keeps objects in memory. Used by default in every test, see [TestFileStorageExtension].
 */
class InMemoryFileStorage : FileStorage {
    class StoredObject(val bytes: ByteArray, val contentType: String)

    val objects = ConcurrentHashMap<String, StoredObject>()

    override val description: String = "memory"

    override fun put(key: String, bytes: ByteArray, contentType: String) {
        objects[key] = StoredObject(bytes.copyOf(), contentType)
    }

    override fun put(key: String, input: InputStream, size: Long, contentType: String) {
        val bytes = input.readNBytes(Math.toIntExact(size))
        check(bytes.size.toLong() == size) { "Expected $size bytes for $key, got ${bytes.size}" }
        put(key, bytes, contentType)
    }

    override fun open(key: String): InputStream =
        ByteArrayInputStream(objects[key]?.bytes ?: throw StoredObjectNotFoundException(key))

    override fun head(key: String): StoredObjectInfo? = objects[key]?.let { StoredObjectInfo(it.bytes.size.toLong()) }

    override fun delete(key: String) {
        objects.remove(key)
    }

    override fun keys(prefix: String): List<String> = objects.keys.filter { it.startsWith(prefix) }.sorted()

    override fun checkAvailable() {}
}
