package org.centrexcursionistalcoi.app.storage.fs

import io.github.vinceglb.filekit.utils.div
import io.ktor.util.cio.use
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.asByteWriteChannel
import io.ktor.utils.io.copyTo
import kotlinx.io.buffered
import kotlinx.io.files.FileNotFoundException
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import org.centrexcursionistalcoi.app.data.DOCUMENTS_PATH
import org.centrexcursionistalcoi.app.data.FILES_PATH
import org.centrexcursionistalcoi.app.data.IMAGES_PATH
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.utils.copyTo

object FileSystem {
    private val fs = SystemFileSystem

    suspend fun write(path: String, channel: ByteReadChannel, progress: (ProgressNotifier)?) {
        val path = SystemDataPath / path
        path.parent?.let { fs.createDirectories(it) }
        fs.sink(path).use { sink ->
            sink.asByteWriteChannel().use {
                if (progress != null) channel.copyTo(this, progress)
                else channel.copyTo(this)
            }
        }
    }

    fun read(path: String, progress: (ProgressNotifier)? = null): ByteArray {
        return fs.source(SystemDataPath / path).use { source ->
            source.buffered().readByteArray()
        }
    }

    fun exists(path: String, progress: (ProgressNotifier)? = null): Boolean {
        return fs.exists(SystemDataPath / path)
    }

    /**
     * Deletes all the files in [path] recursively.
     * @param path The path to delete.
     * @param failOnNotFound Whether to throw an exception if [path] does not exist.
     * @return The number of deleted files and directories.
     * @throws FileNotFoundException if [path] does not exist and [failOnNotFound] is true.
     */
    fun deleteRecursively(path: Path, failOnNotFound: Boolean = false): Int {
        if (!fs.exists(path)) {
            if (failOnNotFound) {
                throw FileNotFoundException("Could not find $path")
            } else {
                return 0
            }
        }
        val metadata = fs.metadataOrNull(path) ?: error("Could not fetch metadata for $path")
        if (metadata.isDirectory) {
            val count = fs.list(path).sumOf { child -> deleteRecursively(child, failOnNotFound) }
            fs.delete(path, mustExist = failOnNotFound)
            return count + 1
        } else {
            fs.delete(path, mustExist = failOnNotFound)
            return 1
        }
    }

    fun deleteAll(): Int {
        var count = deleteRecursively(SystemDataPath / DOCUMENTS_PATH, failOnNotFound = false)
        count += deleteRecursively(SystemDataPath / IMAGES_PATH, failOnNotFound = false)
        count += deleteRecursively(SystemDataPath / FILES_PATH, failOnNotFound = false)
        return count
    }
}
