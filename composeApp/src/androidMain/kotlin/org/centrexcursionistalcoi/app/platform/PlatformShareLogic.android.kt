package org.centrexcursionistalcoi.app.platform

import android.content.Intent
import androidx.core.net.toUri
import io.ktor.http.ContentType
import io.ktor.http.fileExtensions
import java.io.File
import org.centrexcursionistalcoi.app.MainActivity

actual object PlatformShareLogic {
    actual val sharingSupported: Boolean = false

    // TODO: Verify that this works
    actual fun share(data: ByteArray, contentType: ContentType) {
        val context = requireNotNull(MainActivity.instance) { "MainActivity is not instantiated" }

        // Store the data into a temporary file and get a content URI using FileProvider
        val file = File.createTempFile("share", contentType.fileExtensions()[0], context.cacheDir)
        file.outputStream().use {
            it.write(data)
            it.flush()
        }

        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, file.toUri())
            type = contentType.toString()
        }
        context.startActivity(Intent.createChooser(intent, null))
    }
}
