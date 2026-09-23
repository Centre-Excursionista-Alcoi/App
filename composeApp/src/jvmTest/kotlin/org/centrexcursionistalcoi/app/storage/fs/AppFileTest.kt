package org.centrexcursionistalcoi.app.storage.fs

import io.ktor.http.ContentType
import java.io.File
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.io.files.Path
import org.centrexcursionistalcoi.app.di.PathsProvider
import org.junit.AfterClass
import org.junit.BeforeClass
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/**
 * [org.centrexcursionistalcoi.app.di.globalPathsProvider]'s `by inject()` delegate is a [Lazy] cached forever on
 * the process-wide [org.centrexcursionistalcoi.app.di.PathsProviderHolder] object, resolved on its *first* access
 * -- restarting Koin with a *different* fake [PathsProvider] between test methods (as `@BeforeTest`/`@AfterTest`
 * would do) has no effect after that first resolution, silently pointing every later test at the first test's
 * temp directory. Starting Koin once for the whole class, against one shared temp directory, sidesteps that.
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
}
