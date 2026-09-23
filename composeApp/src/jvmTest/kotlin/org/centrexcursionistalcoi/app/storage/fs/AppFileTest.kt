package org.centrexcursionistalcoi.app.storage.fs

import io.ktor.http.ContentType
import io.ktor.utils.io.ByteReadChannel
import java.io.File
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.io.files.Path
import org.centrexcursionistalcoi.app.di.PathsProvider
import org.junit.AfterClass
import org.junit.BeforeClass
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/**
 * Starts Koin once for the whole class (rather than per test method) purely to avoid the overhead of repeated
 * start/stop cycles -- [org.centrexcursionistalcoi.app.di.globalPathsProvider] re-resolves against whichever
 * Koin instance is currently active, so this is a performance choice, not a correctness requirement.
 */
class AppFileTest {
    companion object {
        private val tempDir = File(System.getProperty("java.io.tmpdir"), "AppFileTest-${UUID.randomUUID()}").apply { mkdirs() }

        @BeforeClass
        @JvmStatic
        fun setUpKoin() {
            startKoin {
                modules(
                    module {
                        single<PathsProvider> {
                            object : PathsProvider {
                                override val systemDataPath: Path get() = Path(tempDir.absolutePath)
                            }
                        }
                    }
                )
            }
        }

        @AfterClass
        @JvmStatic
        fun tearDownKoin() {
            stopKoin()
            tempDir.deleteRecursively()
        }
    }

    @AfterTest
    fun tearDown() {
        tempDir.listFiles()?.forEach { it.deleteRecursively() }
    }

    @Test
    fun absolutePath_resolvesRelativeToSystemDataPath() {
        val file = AppFile("ReferencedMemory/document.pdf")

        assertEquals(File(tempDir, "ReferencedMemory/document.pdf").absolutePath, file.absolutePath.toString())
    }

    @Test
    fun exists_reflectsTheRealFileSystem() {
        val file = AppFile("document.pdf")
        assertFalse(file.exists())

        File(tempDir, "document.pdf").writeText("content")

        assertTrue(file.exists())
    }

    @Test
    fun toKmpFile_wrapsTheResolvedFile() {
        val file = AppFile("document.pdf")
        File(tempDir, "document.pdf").writeText("content")

        val kmpFile = file.toKmpFile(ContentType.Application.Pdf)

        assertEquals(file.absolutePath.toString(), kmpFile.file.absolutePath)
        assertEquals("content", kmpFile.file.readText())
    }

    @Test
    fun read_readsTheResolvedFilesContents() = runTest {
        val file = AppFile("document.pdf")
        File(tempDir, "document.pdf").writeText("content")

        val bytes = file.read()

        assertEquals("content", bytes.decodeToString())
    }

    @Test
    fun write_writesTheChannelContentsToTheResolvedFile() = runTest {
        val file = AppFile("ReferencedMemory/document.pdf")

        file.write(ByteReadChannel("content"))

        assertEquals("content", File(tempDir, "ReferencedMemory/document.pdf").readText())
    }
}
