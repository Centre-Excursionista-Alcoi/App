package org.centrexcursionistalcoi.app.storage.fs

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.ktor.http.ContentType
import org.centrexcursionistalcoi.app.data.DOCUMENTS_PATH
import org.centrexcursionistalcoi.app.data.FILES_PATH
import org.centrexcursionistalcoi.app.data.IMAGES_PATH
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/**
 * `provider_paths.xml`'s `<files-path>` entries are hand-maintained strings, and nothing enforces at compile
 * time that they match the directory names files are actually cached under
 * ([org.centrexcursionistalcoi.app.data.DOCUMENTS_PATH]/[IMAGES_PATH]/[FILES_PATH], and
 * `PlatformDragAndDrop.android.kt`'s `"qr"` literal). A typo there -- exactly what happened here, `path="document/"`
 * instead of `path="documents/"` -- doesn't fail until a real file under that directory is actually shared, as
 * `IllegalArgumentException("Failed to find configured root that contains ...")`.
 *
 * This exercises [FilePermissionsUtil.uriForFile] (the same call [AppFile.contentUri]/[AppFile.toKmpFile] make)
 * for a real file under every one of those directories, so a mismatch on any of them fails immediately instead
 * of waiting for whichever one happens to be hit first in the field.
 */
@RunWith(AndroidJUnit4::class)
class ProviderPathsInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun assertShareable(directory: String) {
        val dir = File(context.filesDir, "$directory/${this::class.simpleName}")
        dir.mkdirs()
        val file = File(dir, "${UUID.randomUUID()}.pdf")
        try {
            file.writeText("content")
            val uri = FilePermissionsUtil.uriForFile(context, file, ContentType.Application.Pdf)
            val bytes = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
            assertEquals("content", String(bytes))
        } finally {
            file.delete()
            dir.delete()
        }
    }

    @Test
    fun documentsDirectory_isShareable() = assertShareable(DOCUMENTS_PATH)

    @Test
    fun imagesDirectory_isShareable() = assertShareable(IMAGES_PATH)

    @Test
    fun filesDirectory_isShareable() = assertShareable(FILES_PATH)

    @Test
    fun qrDirectory_isShareable() = assertShareable("qr")
}
