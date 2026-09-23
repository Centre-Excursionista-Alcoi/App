package org.centrexcursionistalcoi.app.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import com.diamondedge.logging.logging
import io.ktor.http.*
import org.centrexcursionistalcoi.app.storage.fs.AppFile
import org.centrexcursionistalcoi.app.storage.fs.contentUri
import org.koin.core.annotation.Singleton

@Singleton
actual class PlatformOpenFileLogic(
    private val context: Context,
) : PlatformProvider {
    private val log = logging()

    actual override val isSupported: Boolean = true

    actual fun open(file: AppFile, contentType: ContentType) {
        // Get a content URI using FileProvider, copying under a name with the proper extension only if needed
        val uri = file.contentUri(context, contentType)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, contentType.toString())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            val chooser = Intent.createChooser(intent, null).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: ActivityNotFoundException) {
            log.e(e) { "View not supported for $file as $contentType" }
        }
    }
}
