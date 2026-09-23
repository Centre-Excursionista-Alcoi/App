package org.centrexcursionistalcoi.app.platform

import io.ktor.http.ContentType
import kotlinx.io.files.Path
import org.centrexcursionistalcoi.app.di.PathsProvider
import java.io.File
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformKmpFileLogicTest {
    private val tempDir = File(System.getProperty("java.io.tmpdir"), "PlatformKmpFileLogicTest-${UUID.randomUUID()}").apply { mkdirs() }
    private val pathsProvider = object : PathsProvider {
        override val systemDataPath: Path get() = Path(tempDir.absolutePath)
    }
    private val logic = PlatformKmpFileLogic(pathsProvider)

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun kmpFile_wrapsTheFile_atTheResolvedPath() {
        val file = File(tempDir, "document.pdf").apply { writeText("content") }

        val kmpFile = logic.kmpFile("document.pdf", ContentType.Application.Pdf)

        assertEquals(file.absolutePath, kmpFile.file.absolutePath)
        assertEquals("content", kmpFile.file.readText())
    }

    @Test
    fun kmpFile_resolvesNestedRelativePaths() {
        val dir = File(tempDir, "ReferencedMemory").apply { mkdirs() }
        val file = File(dir, "document.pdf").apply { writeText("content") }

        val kmpFile = logic.kmpFile("ReferencedMemory/document.pdf", ContentType.Application.Pdf)

        assertEquals(file.absolutePath, kmpFile.file.absolutePath)
    }
}
