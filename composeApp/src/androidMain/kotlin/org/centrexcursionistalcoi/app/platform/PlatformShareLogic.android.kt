package org.centrexcursionistalcoi.app.platform

import android.content.Intent
import com.diamondedge.logging.logging
import io.github.vinceglb.filekit.utils.div
import io.ktor.http.ContentType
import org.centrexcursionistalcoi.app.android.MainActivity
import org.centrexcursionistalcoi.app.storage.fs.FilePermissionsUtil
import org.centrexcursionistalcoi.app.storage.fs.SystemDataPath
import java.io.File

actual object PlatformShareLogic : PlatformProvider {
    private val log = logging()

    actual override val isSupported: Boolean = true

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
        } ?: log.e { "Sharing not supported for $path as $contentType" }
    }

    actual fun share(text: String) {
        val context = requireNotNull(MainActivity.instance) { "MainActivity is not instantiated" }

        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        intent.resolveActivity(context.packageManager)?.let {
            context.startActivity(Intent.createChooser(intent, null))
        } ?: log.e { "Sharing not supported for text" }
    }
}
