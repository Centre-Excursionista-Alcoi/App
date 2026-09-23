package org.centrexcursionistalcoi.app.storage.fs

import com.mohamedrejeb.calf.io.KmpFile
import io.ktor.http.ContentType
import java.io.File

fun AppFile.toJavaFile(): File = File(absolutePath.toString())

actual fun AppFile.toKmpFile(contentType: ContentType): KmpFile = KmpFile(toJavaFile())
