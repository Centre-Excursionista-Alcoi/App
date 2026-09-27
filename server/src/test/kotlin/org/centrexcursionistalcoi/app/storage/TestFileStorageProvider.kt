package org.centrexcursionistalcoi.app.storage

import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import org.junit.jupiter.api.io.TempDir

class TestFileStorageProvider {
    @TempDir
    lateinit var directory: Path

    private val variables = listOf("S3_ENDPOINT", "S3_BUCKET", "S3_ACCESS_KEY_ID", "S3_SECRET_ACCESS_KEY", "S3_REGION", "FILES_PATH")

    private fun configureS3() {
        FileStorageConfig.override("S3_ENDPOINT", "https://account.r2.cloudflarestorage.com")
        FileStorageConfig.override("S3_BUCKET", "bucket")
        FileStorageConfig.override("S3_ACCESS_KEY_ID", "id")
        FileStorageConfig.override("S3_SECRET_ACCESS_KEY", "secret")
    }

    @AfterTest
    fun tearDown() {
        variables.forEach { FileStorageConfig.override(it, null) }
    }

    @Test
    fun test_production_withoutS3_refusesToStart() {
        val error = assertFailsWith<IllegalStateException> { FileStorageProvider.fromConfig(isDevelopment = false) }
        assertContains(error.message.orEmpty(), "S3_ENDPOINT")
    }

    @Test
    fun test_partialConfiguration_refusesToStart() {
        FileStorageConfig.override("S3_ENDPOINT", "https://account.r2.cloudflarestorage.com")
        FileStorageConfig.override("S3_BUCKET", "bucket")

        assertFailsWith<IllegalStateException> { FileStorageProvider.fromConfig(isDevelopment = true) }
        assertFailsWith<IllegalStateException> { FileStorageProvider.fromConfig(isDevelopment = false) }
    }

    @Test
    fun test_development_withoutS3_usesLocalDirectory() {
        FileStorageConfig.override("FILES_PATH", directory.toString())

        val storage = FileStorageProvider.fromConfig(isDevelopment = true)
        assertIs<LocalFileStorage>(storage)
        assertContains(storage.description, directory.toString())
    }

    @Test
    fun test_withS3_usesS3() {
        configureS3()

        for (isDevelopment in listOf(true, false)) {
            FileStorageProvider.fromConfig(isDevelopment).use { storage ->
                assertIs<S3FileStorage>(storage)
                assertEquals("s3://bucket at https://account.r2.cloudflarestorage.com", storage.description)
            }
        }
    }

    @Test
    fun test_region_defaultsToAuto() {
        assertEquals("auto", FileStorageConfig.region)
        FileStorageConfig.override("S3_REGION", "eu")
        assertEquals("eu", FileStorageConfig.region)
    }

    @Test
    fun test_init_checksAvailability() {
        // Unavailable (no such server): init must fail instead of starting without a working storage
        configureS3()
        FileStorageConfig.override("S3_ENDPOINT", "http://127.0.0.1:1")

        assertFailsWith<Exception> { FileStorageProvider.init(isDevelopment = false) }
    }
}
