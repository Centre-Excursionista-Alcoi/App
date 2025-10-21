package org.centrexcursionistalcoi.app.storage.fs

import io.github.vinceglb.filekit.utils.toPath
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.files.Path
import platform.Foundation.*

@OptIn(ExperimentalForeignApi::class)
fun getAppDataDirectory(): String {
    val paths = NSSearchPathForDirectoriesInDomains(
        NSApplicationSupportDirectory,
        NSUserDomainMask,
        true
    )
    val appSupportPath = paths.firstOrNull() as? String
        ?: throw IllegalStateException("Unable to access Application Support directory")

    // Ensure directory exists
    val fileManager = NSFileManager.defaultManager
    if (!fileManager.fileExistsAtPath(appSupportPath)) {
        fileManager.createDirectoryAtPath(
            path = appSupportPath,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )
    }

    return appSupportPath
}

actual val SystemDataPath: Path
    get() = getAppDataDirectory().toPath()
