package org.centrexcursionistalcoi.app.log

import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.io.files.Path
import org.centrexcursionistalcoi.app.data.FileWithContext

class TestFileLogger {
    private val dir = Files.createTempDirectory("file-logger-test")
    private val file = dir.resolve("app.log")
    private val rotated = dir.resolve("app.log.1")

    private fun logger(maxFileBytes: Long = FileLogger.MAX_FILE_BYTES, maxMessageChars: Int = FileLogger.MAX_MESSAGE_CHARS) =
        FileLogger(Path(file.toString()), maxFileBytes, maxMessageChars)

    @Test
    fun `appends to what the file already has`() = runTest {
        file.toFile().writeText("old line\n")
        val logger = logger()
        logger.info("Tag", "hello")
        logger.flush()

        val text = file.readText()
        assertTrue(text.startsWith("old line\n"))
        assertTrue("I/Tag" in text && "hello" in text)
        file.deleteIfExists()
    }

    @Test
    fun `cuts messages that are too long`() = runTest {
        val logger = logger(maxMessageChars = 100)
        logger.debug("Tag", "x".repeat(10_000))
        logger.flush()

        val text = file.readText()
        assertTrue("[truncated, 10000 characters]" in text)
        assertTrue(text.length < 1_000)
    }

    @Test
    fun `rotates the file instead of growing it`() = runTest {
        val logger = logger(maxFileBytes = 2_000, maxMessageChars = 500)
        repeat(50) { logger.info("Tag", "message $it ${"y".repeat(200)}") }
        logger.flush()

        assertTrue(file.exists())
        assertTrue(rotated.exists())
        assertTrue(file.fileSize() <= 2_000, "Size: ${file.fileSize()}")
        assertTrue(rotated.fileSize() <= 2_000, "Rotated size: ${rotated.fileSize()}")
        // The last message is in the current file, and the first has been dropped
        assertTrue("message 49" in file.readText())
        assertFalse("message 0 " in file.readText() + rotated.readText())
    }

    @Test
    fun `printing a file does not include its contents`() {
        val text = FileWithContext(bytes = ByteArray(1_000_000) { 7 }, name = "photo.jpg").toString()
        assertTrue("photo.jpg" in text)
        assertTrue("1000000 B" in text)
        assertEquals(true, text.length < 200)
    }
}
