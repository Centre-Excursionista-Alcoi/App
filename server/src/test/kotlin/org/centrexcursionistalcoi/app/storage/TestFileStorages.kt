package org.centrexcursionistalcoi.app.storage

import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.createDirectories
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.setPosixFilePermissions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.io.TempDir
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

class TestInMemoryFileStorage : FileStorageContractTest() {
    override fun createStorage(): FileStorage = InMemoryFileStorage()
}

class TestLocalFileStorage : FileStorageContractTest() {
    @TempDir
    lateinit var directory: Path

    override fun createStorage(): FileStorage = LocalFileStorage(directory.resolve("files"))

    @Test
    fun test_rejectsKeysOutsideTheDirectory() {
        for (key in listOf("../outside", "files/../../outside", "/absolute", "files//double", "", "files/./a")) {
            assertFailsWith<IllegalArgumentException>("Key \"$key\" must be rejected") {
                storage.put(key, byteArrayOf(1), "application/octet-stream")
            }
        }
        assertEquals(emptyList(), directory.listDirectoryEntries().filter { it.fileName.toString() != "files" })
    }

    @Test
    fun test_checkAvailable_notWritable() {
        val readOnly = directory.resolve("read-only").createDirectories()
        readOnly.setPosixFilePermissions(setOf(java.nio.file.attribute.PosixFilePermission.OWNER_READ, java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE))
        try {
            assertFailsWith<IllegalStateException> { LocalFileStorage(readOnly.resolve("files")).checkAvailable() }
        } finally {
            readOnly.setPosixFilePermissions(java.nio.file.attribute.PosixFilePermission.entries.toSet())
        }
    }
}

/**
 * Runs against a real S3 API (Adobe S3Mock), configured like Cloudflare R2 would be. Heavy test: needs Docker.
 */
class TestS3FileStorage : FileStorageContractTest() {
    companion object {
        const val S3_MOCK_IMAGE = "adobe/s3mock:5.2.3"
        private const val PORT = 9090

        private val container: GenericContainer<*> by lazy {
            GenericContainer(S3_MOCK_IMAGE)
                .withExposedPorts(PORT)
                .waitingFor(Wait.forListeningPort())
                .apply { start() }
        }

        fun endpoint() = "http://${container.host}:${container.getMappedPort(PORT)}"

        /**
         * Creates a storage using a new, empty bucket.
         */
        fun newStorage(): S3FileStorage {
            val bucket = "test-${UUID.randomUUID()}"
            return S3FileStorage.create(endpoint(), bucket, accessKeyId = "test", secretAccessKey = "test").also {
                it.createBucket()
            }
        }
    }

    override fun createStorage(): FileStorage = newStorage()

    @Test
    fun test_checkAvailable_missingBucket() {
        val storage = S3FileStorage.create(endpoint(), "missing-bucket", accessKeyId = "test", secretAccessKey = "test")
        storage.use {
            assertFailsWith<IllegalStateException> { it.checkAvailable() }
        }
    }
}
