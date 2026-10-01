package org.centrexcursionistalcoi.app.request

import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.centrexcursionistalcoi.app.ResourcesUtils
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.plugins.CallUploads
import org.centrexcursionistalcoi.app.storage.FileStorageProvider
import org.centrexcursionistalcoi.app.storage.LocalFileStorage
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.io.TempDir

class TestFileRequestData {
    @TempDir
    lateinit var directory: Path

    @BeforeTest
    fun setUp() {
        Database.initForTests()
    }

    @AfterTest
    fun tearDown() {
        Database.clear()
    }

    /** A file part whose contents are generated as they are read: [head], then [size] - head.size bytes. */
    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
    private fun fileItem(head: ByteArray, size: Long, name: String = "big.pdf"): PartData.FileItem {
        val provider = {
            GlobalScope.writer {
                channel.writeFully(head)
                val chunk = ByteArray(1024 * 1024) { it.toByte() }
                var remaining = size - head.size
                while (remaining > 0) {
                    val count = minOf(remaining, chunk.size.toLong()).toInt()
                    channel.writeFully(chunk, 0, count)
                    remaining -= count
                }
            }.channel as ByteReadChannel
        }
        val headers = Headers.build {
            append(HttpHeaders.ContentDisposition, ContentDisposition.File.withParameter(ContentDisposition.Parameters.FileName, name).toString())
        }
        return PartData.FileItem(provider, {}, headers)
    }

    private fun uploadTempFiles(): List<Path> =
        Files.list(Path.of(System.getProperty("java.io.tmpdir"))).use { files ->
            files.filter { it.fileName.toString().startsWith("cea-upload-") }.toList()
        }

    @Test
    fun test_largerThanTheHeap_isStreamedToTheStorage() = runTest(timeout = kotlin.time.Duration.parse("5m")) {
        // Keep the test reasonably light: it writes the file twice to disk
        val maxHeap = Runtime.getRuntime().maxMemory()
        assumeTrue(maxHeap <= 1024L * 1024 * 1024, "Needs a heap of at most 1 GiB to prove anything")
        FileStorageProvider.useForTests(LocalFileStorage(directory.resolve("storage")))

        val pdfHead = ResourcesUtils.bytesFromResource("/document.pdf").copyOf(16)
        val size = maxHeap + 64L * 1024 * 1024
        val before = uploadTempFiles()

        val upload = FileRequestData()
        upload.populate(fileItem(pdfHead, size))
        assertEquals(size, upload.size)

        val file = upload.newEntity()

        Database {
            assertEquals(size, file.size)
            // Detected from the first bytes, without reading the rest
            assertEquals(ContentType.Application.Pdf, file.contentType)
            val stored = directory.resolve("storage").resolve(file.objectKey)
            assertEquals(size, stored.fileSize())
            assertContentEquals(pdfHead, stored.inputStream().use { it.readNBytes(pdfHead.size) })
        }
        assertEquals(before, uploadTempFiles(), "The temporary file must be deleted once stored")
    }

    @Test
    fun test_closedWithTheCall() = runTest {
        val before = uploadTempFiles()
        val uploads = CallUploads()

        withContext(uploads) {
            FileRequestData().populate(fileItem(byteArrayOf(1, 2, 3), 1024))
        }
        assertEquals(before.size + 1, uploadTempFiles().size)

        // What the plugin does once the call has been handled
        uploads.closeAll()
        assertEquals(before, uploadTempFiles())
    }

    @Test
    fun test_empty() = runTest {
        val upload = FileRequestData()
        assertTrue(upload.isEmpty())
        assertEquals(0, upload.readBytes().size)
        upload.close()
        upload.close()
    }
}
