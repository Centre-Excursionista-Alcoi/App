package storage

import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import java.io.File


actual object AppFileSystem {
    /**
     * The filesystem to be used. `SystemFileSystem` should be enough.
     */
    actual val fileSystem: FileSystem = SystemFileSystem

    /**
     * The character used to separate paths in the filesystem.
     */
    actual val pathDivider: Char = File.separatorChar

    /**
     * The root directory of the filesystem, where all the app data will be stored at.
     */
    actual val root: Path by lazy { getAppData() }

    private fun getAppData(): Path {
        val root = System.getenv("APPDATA") ?: System.getProperty("user.home")
        return root!!.let { appData ->
            Path(appData, ".cea").also {
                fileSystem.createDirectories(it)
            }
        }
    }
}
