package storage

import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path

expect object AppFileSystem {
    /**
     * The filesystem to be used. `SystemFileSystem` should be enough.
     */
    val fileSystem: FileSystem

    /**
     * The character used to separate paths in the filesystem.
     */
    val pathDivider: Char

    /**
     * The root directory of the filesystem, where all the app data will be stored at.
     */
    val root: Path
}
