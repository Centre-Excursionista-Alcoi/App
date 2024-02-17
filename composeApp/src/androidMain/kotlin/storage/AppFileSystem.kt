package storage

import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.centrexcursionistalcoi.app.applicationContext

actual object AppFileSystem {
    /**
     * The filesystem to be used. `SystemFileSystem` should be enough.
     */
    actual val fileSystem: FileSystem = SystemFileSystem

    /**
     * The character used to separate paths in the filesystem.
     */
    actual val pathDivider: Char = '/'

    /**
     * The root directory of the filesystem, where all the app data will be stored at.
     */
    actual val root: Path by lazy {
        Path(applicationContext.filesDir.toString())
    }
}
