package org.centrexcursionistalcoi.app.storage.fs

import android.content.Context
import android.net.Uri
import com.mohamedrejeb.calf.io.KmpFile
import io.ktor.http.ContentType
import java.io.File
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

fun AppFile.toJavaFile(): File = File(absolutePath.toString())

/** A `content://` URI granting read access to this file through the app's own `FileProvider`. */
fun AppFile.contentUri(context: Context, contentType: ContentType): Uri =
    FilePermissionsUtil.uriForFile(context, toJavaFile(), contentType)

private object ContextHolder : KoinComponent {
    val context: Context by inject()
}

actual fun AppFile.toKmpFile(contentType: ContentType): KmpFile = KmpFile(contentUri(ContextHolder.context, contentType))
