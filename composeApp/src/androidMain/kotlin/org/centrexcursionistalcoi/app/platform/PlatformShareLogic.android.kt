package org.centrexcursionistalcoi.app.platform

import android.content.Intent
import io.github.aakira.napier.Napier
import io.github.vinceglb.filekit.utils.div
import io.ktor.http.ContentType
import java.io.File
import org.centrexcursionistalcoi.app.MainActivity
import org.centrexcursionistalcoi.app.storage.fs.FilePermissionsUtil
import org.centrexcursionistalcoi.app.storage.fs.SystemDataPath

actual object PlatformShareLogic {
    actual val sharingSupported: Boolean = true

    actual fun share(path: String, contentType: ContentType) {
        val context = requireNotNull(MainActivity.instance) { "MainActivity is not instantiated" }

        // Store the data into a symbolic link with proper extension and get a content URI using FileProvider
        val filePath = SystemDataPath / path
        val file = File(filePath.toString())
        val uri = FilePermissionsUtil.uriForFile(context, file, contentType)

        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = contentType.toString()
        }
        intent.resolveActivity(context.packageManager)?.let {
            context.startActivity(Intent.createChooser(intent, null))
        } ?: Napier.e { "Sharing not supported for $path as $contentType" }
    }
}
