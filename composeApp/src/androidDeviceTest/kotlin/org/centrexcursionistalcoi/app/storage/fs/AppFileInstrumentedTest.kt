package org.centrexcursionistalcoi.app.storage.fs

import android.content.Context
import android.provider.OpenableColumns
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.ktor.http.ContentType
import java.io.File
import java.io.FileNotFoundException
import java.util.UUID
import kotlinx.io.files.Path
import org.centrexcursionistalcoi.app.data.DOCUMENTS_PATH
import org.centrexcursionistalcoi.app.di.PathsProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/**
 * Runs on a real device/emulator, like [FilePermissionsUtilInstrumentedTest]/[ProviderPathsInstrumentedTest]
 * (whose [FilePermissionsUtil] [AppFile.contentUri]/[AppFile.toKmpFile] are thin wrappers around): a host-JVM
 * test can't exercise the real `FileProvider` ContentProvider this depends on.
 *
 * The whole reason [AppFile.toKmpFile] goes through the app's own `FileProvider` on Android -- rather than
 * Calf's own `File.toKmpFile()`, which builds a plain `file://` URI via `Uri.fromFile()` -- is that the URI it
 * returns must actually grant a real, working read permission through the app's `FileProvider`, not just be
 * *some* URI. These tests verify that directly.
 */
@RunWith(AndroidJUnit4::class)
class AppFileInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    // Mirrors how a real ReferencedMemory/UserInsurance/... document is laid out (see FileContainerExtensions.kt's
    // fetchDocumentFilePath) -- this is the exact directory whose provider_paths.xml entry was mismatched
    // ("document/" vs "documents/"), so this test would have caught that specific regression.
    private val relativeDir = "$DOCUMENTS_PATH/${this::class.simpleName}"
    private lateinit var relativePath: String
    private lateinit var file: File

    @Before
    fun setUp() {
        startKoin {
            modules(
                module {
                    single<Context> { context }
                    single<PathsProvider> {
                        object : PathsProvider {
                            override val systemDataPath: Path get() = Path(context.filesDir.absolutePath)
                        }
                    }
                }
            )
        }

        val dir = File(context.filesDir, relativeDir)
        dir.mkdirs()
        file = File(dir, UUID.randomUUID().toString())
        file.writeText("fake pdf content")
        relativePath = "$relativeDir/${file.name}"
    }

    @After
    fun tearDown() {
        stopKoin()
        file.parentFile?.listFiles()?.forEach { it.delete() }
    }

    @Test
    fun toKmpFile_wrapsAContentUri_notAFileUri() {
        val kmpFile = AppFile(relativePath).toKmpFile(ContentType.Application.Pdf)

        // A file:// URI would "resolve" here too -- right up until ShareLauncher puts it in a real share Intent
        // and Android throws FileUriExposedException. content:// is the only scheme that's actually shareable.
        assertEquals("content", kmpFile.uri.scheme)
    }

    @Test
    fun toKmpFile_uriGrantsRealReadAccess_throughTheContentResolver() {
        val kmpFile = AppFile(relativePath).toKmpFile(ContentType.Application.Pdf)

        val bytes = context.contentResolver.openInputStream(kmpFile.uri)!!.use { it.readBytes() }
        assertEquals("fake pdf content", String(bytes))
    }

    @Test
    fun toKmpFile_displayName_hasTheContentTypesExtension() {
        val kmpFile = AppFile(relativePath).toKmpFile(ContentType.Application.Pdf)

        context.contentResolver.query(kmpFile.uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)!!.use { cursor ->
            assertTrue(cursor.moveToFirst())
            val name = cursor.getString(0)
            assertTrue("Expected a .pdf display name, got: $name", name.endsWith(".pdf"))
        }
    }

    @Test(expected = FileNotFoundException::class)
    fun toKmpFile_aPathThatDoesNotExist_throws() {
        AppFile("$relativeDir/does-not-exist").toKmpFile(ContentType.Application.Pdf)
    }
}
