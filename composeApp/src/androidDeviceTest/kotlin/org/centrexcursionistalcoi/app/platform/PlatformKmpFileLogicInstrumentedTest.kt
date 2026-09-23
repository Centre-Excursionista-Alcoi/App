package org.centrexcursionistalcoi.app.platform

import android.content.Context
import android.provider.OpenableColumns
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.ktor.http.ContentType
import org.centrexcursionistalcoi.app.data.DOCUMENTS_PATH
import org.centrexcursionistalcoi.app.di.AndroidPathsProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileNotFoundException
import java.util.UUID

/**
 * Runs on a real device/emulator, like [org.centrexcursionistalcoi.app.storage.fs.FilePermissionsUtilInstrumentedTest]
 * (whose [org.centrexcursionistalcoi.app.storage.fs.FilePermissionsUtil] this class's Android actual is a thin
 * wrapper around): a host-JVM test can't exercise the real `FileProvider` ContentProvider this depends on.
 *
 * The whole reason [PlatformKmpFileLogic] exists on Android -- rather than Calf's own `File.toKmpFile()`, which
 * builds a plain `file://` URI via `Uri.fromFile()` -- is that the URI it returns must actually grant a real,
 * working read permission through the app's `FileProvider`, not just be *some* URI. These tests verify that
 * directly: the returned [com.mohamedrejeb.calf.io.KmpFile] must wrap a `content://` URI whose bytes and display
 * name are genuinely readable back out through the [android.content.ContentResolver], exactly as a receiving app
 * (the whole point of sharing) would read them.
 */
@RunWith(AndroidJUnit4::class)
class PlatformKmpFileLogicInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val logic = PlatformKmpFileLogic(context, AndroidPathsProvider(context))

    // Mirrors how a real ReferencedMemory/UserInsurance/... document is laid out (see FileContainerExtensions.kt's
    // fetchDocumentFilePath) -- this is the exact directory whose provider_paths.xml entry was mismatched
    // ("document/" vs "documents/"), so this test would have caught that specific regression.
    private val relativeDir = "$DOCUMENTS_PATH/${this::class.simpleName}"
    private lateinit var relativePath: String
    private lateinit var file: File

    @Before
    fun setUp() {
        val dir = File(context.filesDir, relativeDir)
        dir.mkdirs()
        file = File(dir, UUID.randomUUID().toString())
        file.writeText("fake pdf content")
        relativePath = "$relativeDir/${file.name}"
    }

    @After
    fun tearDown() {
        file.parentFile?.listFiles()?.forEach { it.delete() }
    }

    @Test
    fun kmpFile_wrapsAContentUri_notAFileUri() {
        val kmpFile = logic.kmpFile(relativePath, ContentType.Application.Pdf)

        // A file:// URI would "resolve" here too -- right up until ShareLauncher puts it in a real share Intent
        // and Android throws FileUriExposedException. content:// is the only scheme that's actually shareable.
        assertEquals("content", kmpFile.uri.scheme)
    }

    @Test
    fun kmpFile_uriGrantsRealReadAccess_throughTheContentResolver() {
        val kmpFile = logic.kmpFile(relativePath, ContentType.Application.Pdf)

        val bytes = context.contentResolver.openInputStream(kmpFile.uri)!!.use { it.readBytes() }
        assertEquals("fake pdf content", String(bytes))
    }

    @Test
    fun kmpFile_displayName_hasTheContentTypesExtension() {
        val kmpFile = logic.kmpFile(relativePath, ContentType.Application.Pdf)

        context.contentResolver.query(kmpFile.uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)!!.use { cursor ->
            assertTrue(cursor.moveToFirst())
            val name = cursor.getString(0)
            assertTrue("Expected a .pdf display name, got: $name", name.endsWith(".pdf"))
        }
    }

    @Test(expected = FileNotFoundException::class)
    fun kmpFile_aPathThatDoesNotExist_throws() {
        logic.kmpFile("$relativeDir/does-not-exist", ContentType.Application.Pdf)
    }
}
