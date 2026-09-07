package org.centrexcursionistalcoi.app.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import com.diamondedge.logging.logging
import io.github.vinceglb.filekit.utils.div
import io.ktor.http.ContentType
import org.centrexcursionistalcoi.app.storage.fs.FilePermissionsUtil
import org.centrexcursionistalcoi.app.storage.fs.SystemDataPath
import org.koin.core.annotation.Singleton
import java.io.File

@Singleton
actual class PlatformShareLogic(private val context: Context) : PlatformProvider {
    private val log = logging()

    actual override val isSupported: Boolean = true

    actual fun share(path: String, contentType: ContentType) {
        // Store the data into a symbolic link with proper extension and get a content URI using FileProvider
        val filePath = SystemDataPath / path
        val file = File(filePath.toString())
        val uri = FilePermissionsUtil.uriForFile(context, file, contentType)

        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = contentType.toString()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            val chooser = Intent.createChooser(intent, null).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: ActivityNotFoundException) {
            log.e(e) { "Sharing not supported for $path as $contentType" }
        }
    }

    actual fun share(text: String) {
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            val chooser = Intent.createChooser(intent, null).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: ActivityNotFoundException) {
            log.e(e) { "Sharing not supported for text" }
        }
    }
}
