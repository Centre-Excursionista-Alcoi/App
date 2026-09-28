package org.centrexcursionistalcoi.app.storage

import java.io.IOException
import java.io.InputStream
import java.nio.file.Path

/**
 * Wraps [delegate], failing the way a broken storage would.
 * @param failPutOnCall If not null, the put with this number (starting at 1) fails.
 * @param wrongHeadSize If true, [head] reports one byte more than stored.
 * @param unavailable If true, [checkAvailable] fails.
 * @param failDeletes If true, every delete fails.
 */
class FailingFileStorage(
    private val delegate: FileStorage = InMemoryFileStorage(),
    private val failPutOnCall: Int? = null,
    private val wrongHeadSize: Boolean = false,
    private val unavailable: Boolean = false,
    private val failDeletes: Boolean = false,
) : FileStorage by delegate {
    private var puts = 0

    private fun countPut() {
        puts++
        if (puts == failPutOnCall) throw IOException("Simulated failure storing object #$puts")
    }

    override fun put(key: String, bytes: ByteArray, contentType: String) {
        countPut()
        delegate.put(key, bytes, contentType)
    }

    override fun put(key: String, input: InputStream, size: Long, contentType: String) {
        countPut()
        delegate.put(key, input, size, contentType)
    }

    override fun put(key: String, file: Path, contentType: String) {
        countPut()
        delegate.put(key, file, contentType)
    }

    override fun head(key: String): StoredObjectInfo? =
        delegate.head(key)?.let { if (wrongHeadSize) it.copy(size = it.size + 1) else it }

    override fun delete(key: String) {
        if (failDeletes) throw IOException("Simulated failure deleting $key")
        delegate.delete(key)
    }

    override fun checkAvailable() {
        check(!unavailable) { "Simulated unavailable storage" }
        delegate.checkAvailable()
    }
}
