package org.centrexcursionistalcoi.app.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import com.diamondedge.logging.logging
import io.github.vinceglb.filekit.utils.div
import io.ktor.http.*
import org.centrexcursionistalcoi.app.di.PathsProvider
import org.centrexcursionistalcoi.app.storage.fs.FilePermissionsUtil
import org.koin.core.annotation.Singleton
import java.io.File

@Singleton
actual class PlatformOpenFileLogic(
    private val context: Context,
    private val pathsProvider: PathsProvider,
) : PlatformProvider {
    private val log = logging()

    actual override val isSupported: Boolean = true

    actual fun open(path: String, contentType: ContentType) {
        // Copy the data under a name with the proper extension and get a content URI using FileProvider
        val filePath = pathsProvider.systemDataPath / path
        val file = File(filePath.toString())
        val uri = FilePermissionsUtil.uriForFile(context, file, contentType)

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
            log.e(e) { "View not supported for $path as $contentType" }
        }
    }
}
