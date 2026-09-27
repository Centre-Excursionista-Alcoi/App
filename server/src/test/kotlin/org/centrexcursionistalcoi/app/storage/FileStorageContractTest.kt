package org.centrexcursionistalcoi.app.storage

import java.io.ByteArrayInputStream
import java.nio.file.Path
import kotlin.io.path.writeBytes
import org.junit.jupiter.api.io.TempDir
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * The behavior every [FileStorage] must have. Run against each implementation by its subclasses.
 */
abstract class FileStorageContractTest {
    protected abstract fun createStorage(): FileStorage

    protected lateinit var storage: FileStorage

    @BeforeTest
    fun createStorageForTest() {
        storage = createStorage()
    }

    @AfterTest
    fun closeStorage() {
        storage.close()
    }

    @Test
    fun test_putBytes_readBack() {
        val bytes = Random.nextBytes(1024)
        storage.put("files/a", bytes, "application/octet-stream")

        assertContentEquals(bytes, storage.readBytes("files/a"))
        assertContentEquals(bytes, storage.open("files/a").use { it.readBytes() })
        assertEquals(StoredObjectInfo(1024), storage.head("files/a"))
    }

    @Test
    fun test_putStream_readBack() {
        val bytes = Random.nextBytes(4096)
        storage.put("files/b", ByteArrayInputStream(bytes), bytes.size.toLong(), "image/png")

        assertContentEquals(bytes, storage.readBytes("files/b"))
        assertEquals(StoredObjectInfo(4096), storage.head("files/b"))
    }

    @Test
    fun test_putFile_readBack(@TempDir directory: Path) {
        val bytes = Random.nextBytes(3 * 1024 * 1024)
        val file = directory.resolve("upload").also { it.writeBytes(bytes) }
        storage.put("files/from-file", file, "application/pdf")

        assertContentEquals(bytes, storage.readBytes("files/from-file"))
        assertEquals(StoredObjectInfo(bytes.size.toLong()), storage.head("files/from-file"))
    }

    @Test
    fun test_emptyObject() {
        storage.put("files/empty", ByteArray(0), "text/plain")

        assertContentEquals(ByteArray(0), storage.readBytes("files/empty"))
        assertEquals(StoredObjectInfo(0), storage.head("files/empty"))
    }

    @Test
    fun test_largeObject_streamed() {
        val bytes = Random.nextBytes(8 * 1024 * 1024)
        storage.put("files/large", bytes, "application/pdf")

        val read = storage.open("files/large").use { input ->
            val buffer = ByteArray(64 * 1024)
            val output = java.io.ByteArrayOutputStream(bytes.size)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
        assertContentEquals(bytes, read)
    }

    @Test
    fun test_missingObject() {
        assertNull(storage.head("files/missing"))
        assertFailsWith<StoredObjectNotFoundException> { storage.open("files/missing") }
        assertFailsWith<StoredObjectNotFoundException> { storage.readBytes("files/missing") }
    }

    @Test
    fun test_put_overwrites() {
        storage.put("files/c", byteArrayOf(1, 2, 3), "application/octet-stream")
        storage.put("files/c", byteArrayOf(4, 5), "application/octet-stream")

        assertContentEquals(byteArrayOf(4, 5), storage.readBytes("files/c"))
        assertEquals(StoredObjectInfo(2), storage.head("files/c"))
    }

    @Test
    fun test_delete_isIdempotent() {
        storage.put("files/d", byteArrayOf(1), "application/octet-stream")

        storage.delete("files/d")
        assertNull(storage.head("files/d"))
        // Deleting a missing object does nothing
        storage.delete("files/d")
        storage.delete("files/never-existed")
    }

    @Test
    fun test_keys() {
        storage.put("files/1", byteArrayOf(1), "application/octet-stream")
        storage.put("files/2", byteArrayOf(2), "application/octet-stream")
        storage.put("other/3", byteArrayOf(3), "application/octet-stream")

        assertEquals(listOf("files/1", "files/2"), storage.keys("files/").sorted())
        assertEquals(listOf("files/1", "files/2", "other/3"), storage.keys().sorted())
    }

    @Test
    fun test_checkAvailable() {
        storage.checkAvailable()
    }
}
