package org.centrexcursionistalcoi.app.platform

import android.content.Intent
import android.system.Os
import androidx.core.content.FileProvider
import io.github.aakira.napier.Napier
import io.github.vinceglb.filekit.utils.div
import io.ktor.http.ContentType
import io.ktor.http.fileExtensions
import java.io.File
import org.centrexcursionistalcoi.app.MainActivity
import org.centrexcursionistalcoi.app.storage.fs.SystemDataPath

actual object PlatformOpenFileLogic {
    actual val supported: Boolean = true

    actual fun open(path: String, contentType: ContentType) {
        val context = requireNotNull(MainActivity.instance) { "MainActivity is not instantiated" }

        // Store the data into a temporary file and get a content URI using FileProvider
        val filePath = SystemDataPath / path
        var file = File(filePath.toString())
        val extension = contentType.fileExtensions().firstOrNull()
        if (extension != null) {
            val symLinkFilePath = File(file.parentFile, filePath.name + "." + extension)
            if (!symLinkFilePath.exists()) {
                Napier.d { "Creating symlink for $file at $symLinkFilePath" }
                Os.symlink(file.path, symLinkFilePath.path)
            }
            file = symLinkFilePath
        }
        val uri = FileProvider.getUriForFile(context, "org.centrexcursionistalcoi.app.provider", file)

        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            setDataAndType(uri, contentType.toString())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        intent.resolveActivity(context.packageManager)?.let {
            context.startActivity(Intent.createChooser(intent, null))
        } ?: Napier.e { "View not supported for $path as $contentType" }
    }
}
