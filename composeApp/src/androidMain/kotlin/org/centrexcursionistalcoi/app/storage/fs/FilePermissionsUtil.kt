package org.centrexcursionistalcoi.app.storage.fs

import android.content.Context
import android.net.Uri
import android.system.Os
import androidx.core.content.FileProvider
import com.diamondedge.logging.logging
import io.ktor.http.ContentType
import io.ktor.http.fileExtensions
import java.io.File
import java.io.FileNotFoundException

object FilePermissionsUtil {
    private val log = logging()

    fun uriForFile(context: Context, file: File, contentType: ContentType): Uri {
        var sharingFile = file
        if (!file.exists()) {
            log.e { "File to share does not exist: $file" }
            throw FileNotFoundException("File does not exist: $file")
        }
        val extension = contentType.fileExtensions().firstOrNull()
        if (extension != null) {
            val parent: File? = file.parentFile
            if (parent?.exists() != true) parent?.mkdirs()
            val symLinkFilePath = File(parent, file.name + "." + extension)
            if (!symLinkFilePath.exists()) {
                log.d { "Creating symlink for $file at $symLinkFilePath" }
                Os.symlink(file.path, symLinkFilePath.path)
            }
            sharingFile = symLinkFilePath
        }
        return FileProvider.getUriForFile(context, "org.centrexcursionistalcoi.app.provider", sharingFile)
    }
}
