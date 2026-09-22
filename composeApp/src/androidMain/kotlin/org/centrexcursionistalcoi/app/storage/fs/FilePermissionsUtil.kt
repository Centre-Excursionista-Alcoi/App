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
            val namedFilePath = File(parent, file.name + "." + extension)
            // A symlink here would still point at this extensionless file underneath, and an app that resolves
            // the real/canonical path instead of asking FileProvider for the display name -- as some ACTION_SEND
            // receivers do, e.g. WhatsApp/Gmail via /proc/self/fd -- would still see no extension and fall back to
            // a generic binary (#673). A hard link is a second name for the very same data, so its own canonical
            // path already carries the extension; recreated every time so a stale symlink from before this fix
            // can't keep reintroducing the bug for an already-cached file.
            if (namedFilePath.exists()) namedFilePath.delete()
            log.d { "Creating hard link for $file at $namedFilePath" }
            Os.link(file.path, namedFilePath.path)
            sharingFile = namedFilePath
        }
        return FileProvider.getUriForFile(context, "org.centrexcursionistalcoi.app.provider", sharingFile)
    }
}
