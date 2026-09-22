package org.centrexcursionistalcoi.app.storage.fs

import android.content.Context
import android.provider.OpenableColumns
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.ktor.http.ContentType
import java.io.File
import java.io.FileNotFoundException
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs on a real device/emulator because [FilePermissionsUtil] depends on [androidx.core.content.FileProvider] (a
 * real ContentProvider) -- this is exactly the sharing/opening/drag-and-drop path behind #673, and exactly where
 * an earlier version of this fix (a hard link instead of a copy) turned out to fail on-device with EACCES: Android's
 * SELinux policy denies link() for app-private storage, something no host-JVM test could ever have caught. This is
 * the one place that bug -- and the original one -- could have been caught before shipping either time.
 */
@RunWith(AndroidJUnit4::class)
class FilePermissionsUtilInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    // Mirrors how a real cached file is laid out: extensionless, under files/<ClassName>/<uuid>
    // (see FileContainerExtensions.kt), inside a path FileProvider's <paths> config actually claims.
    private lateinit var originalFile: File
    private lateinit var namedFile: File

    @Before
    fun setUp() {
        val dir = File(context.filesDir, "files/InstrumentedTest")
        dir.mkdirs()
        originalFile = File(dir, UUID.randomUUID().toString())
        originalFile.writeText("%PDF-1.4 fake content for a test")
        namedFile = File(dir, "${originalFile.name}.pdf")
    }

    @After
    fun tearDown() {
        originalFile.parentFile?.listFiles()?.forEach { it.delete() }
    }

    @Test
    fun uriForFile_appendsTheContentTypesExtension_toTheDisplayName() {
        val uri = FilePermissionsUtil.uriForFile(context, originalFile, ContentType.Application.Pdf)

        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)!!.use { cursor ->
            assertTrue(cursor.moveToFirst())
            val name = cursor.getString(0)
            assertTrue("Expected a .pdf display name, got: $name", name.endsWith(".pdf"))
        }
    }

    // Regression test for #673: sharing a PDF showed up with no extension in the receiving app. FileProvider was
    // originally pointed at a symlink; that satisfies a receiver that asks FileProvider for the display name, but
    // the symlink's own canonical path still resolved straight through to the extensionless original underneath --
    // and some ACTION_SEND receivers (WhatsApp, Gmail, ...) read that canonical path directly instead. A hard link
    // would have the same canonical path as the correctly-named file itself, but SELinux blocks link() for
    // app-private storage on a real device, so the actual fix is a real, independent copy -- which has no
    // canonical-path relationship to the original at all.
    @Test
    fun uriForFile_namedFilesCanonicalPath_hasTheExtension_andIsNotASymlink() {
        FilePermissionsUtil.uriForFile(context, originalFile, ContentType.Application.Pdf)

        assertTrue(namedFile.exists())
        // What an app that resolves the real/canonical path (rather than asking FileProvider for the display
        // name) would actually see -- this is the exact check that would have failed before the #673 fix.
        assertTrue(namedFile.canonicalFile.name.endsWith(".pdf"))
    }

    @Test
    fun uriForFile_namedFile_isAnIndependentCopy_notTheOriginalItself() {
        FilePermissionsUtil.uriForFile(context, originalFile, ContentType.Application.Pdf)

        assertNotEquals(namedFile.canonicalFile, originalFile.canonicalFile)
        assertEquals(originalFile.readText(), namedFile.readText())
    }

    @Test
    fun uriForFile_streamedContent_matchesTheOriginalFile() {
        val uri = FilePermissionsUtil.uriForFile(context, originalFile, ContentType.Application.Pdf)

        val bytes = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
        assertEquals(originalFile.readText(), String(bytes))
    }

    @Test
    fun uriForFile_calledTwice_recreatesTheCopy_returningTheSameUri() {
        val first = FilePermissionsUtil.uriForFile(context, originalFile, ContentType.Application.Pdf)
        val second = FilePermissionsUtil.uriForFile(context, originalFile, ContentType.Application.Pdf)

        assertEquals(first, second)
    }

    @Test
    fun uriForFile_calledAgainAfterTheOriginalChanged_copiesTheNewContent() {
        FilePermissionsUtil.uriForFile(context, originalFile, ContentType.Application.Pdf)
        originalFile.writeText("updated content")

        FilePermissionsUtil.uriForFile(context, originalFile, ContentType.Application.Pdf)

        assertEquals("updated content", namedFile.readText())
    }

    @Test(expected = FileNotFoundException::class)
    fun uriForFile_aFileThatDoesNotExist_throws() {
        FilePermissionsUtil.uriForFile(context, File(originalFile.parentFile, "does-not-exist"), ContentType.Application.Pdf)
    }

    // Every file cached through FileContainer (FileContainerExtensions.kt) is stored with its extension already,
    // exactly so this case is the common one: no copy, no second file, the original is used directly.
    @Test
    fun uriForFile_aFileAlreadyNamedWithTheExtension_isUsedDirectly_noCopyMade() {
        val alreadyNamed = File(originalFile.parentFile, "${UUID.randomUUID()}.pdf")
        alreadyNamed.writeText("already correctly named")

        val uri = FilePermissionsUtil.uriForFile(context, alreadyNamed, ContentType.Application.Pdf)

        // Not "<name>.pdf.pdf": the extension check has to be a suffix match, not a blind append.
        assertTrue(!File(alreadyNamed.parentFile, "${alreadyNamed.name}.pdf").exists())
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)!!.use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(alreadyNamed.name, cursor.getString(0))
        }
        val bytes = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
        assertEquals("already correctly named", String(bytes))
    }
}
