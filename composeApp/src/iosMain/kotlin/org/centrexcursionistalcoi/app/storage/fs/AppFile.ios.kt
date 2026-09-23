package org.centrexcursionistalcoi.app.storage.fs

import com.mohamedrejeb.calf.io.KmpFile
import io.ktor.http.ContentType
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL

fun AppFile.toNSURL(): NSURL = NSURL.fileURLWithPath(absolutePath.toString())

@OptIn(ExperimentalForeignApi::class)
actual fun AppFile.toKmpFile(contentType: ContentType): KmpFile = KmpFile(toNSURL())
