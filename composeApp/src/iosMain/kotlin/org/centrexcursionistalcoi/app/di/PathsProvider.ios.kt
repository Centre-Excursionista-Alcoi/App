package org.centrexcursionistalcoi.app.di

import io.github.vinceglb.filekit.utils.toPath
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.files.Path
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

class IosPathsProvider : PathsProvider {
    override val systemDataPath: Path
        get() = getAppDataDirectory().toPath()

    @OptIn(ExperimentalForeignApi::class)
    private fun getAppDataDirectory(): String {
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
}
