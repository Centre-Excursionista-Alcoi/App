package org.centrexcursionistalcoi.app.storage.fs

import android.content.Context
import android.net.Uri
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
            // a generic binary (#673). A hard link would avoid the copy, but SELinux denies link() for app-private
            // storage on-device (EACCES, confirmed by FilePermissionsUtilInstrumentedTest) -- a real copy is the
            // only thing that actually works here. Recreated every time so a stale file from before this fix, or
            // simply outdated content, can't stick around.
            log.d { "Copying $file to $namedFilePath so it can be shared under the right extension" }
            file.copyTo(namedFilePath, overwrite = true)
            sharingFile = namedFilePath
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.provider", sharingFile)
    }
}
