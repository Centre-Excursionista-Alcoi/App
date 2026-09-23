package org.centrexcursionistalcoi.app.storage.fs

import io.github.vinceglb.filekit.utils.div
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.io.IOException
import kotlinx.io.buffered
import kotlinx.io.files.FileNotFoundException
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import org.centrexcursionistalcoi.app.data.DOCUMENTS_PATH
import org.centrexcursionistalcoi.app.data.FILES_PATH
import org.centrexcursionistalcoi.app.data.IMAGES_PATH
import org.centrexcursionistalcoi.app.di.globalPathsProvider
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.utils.copyTo

object FileSystem {
    private val fs = SystemFileSystem
    private val systemDataPath get() = globalPathsProvider.systemDataPath

    suspend fun write(file: AppFile, channel: ByteReadChannel, progress: (ProgressNotifier)?) {
        val path = file.absolutePath
        path.parent?.let { fs.createDirectories(it) }
        fs.sink(path).use { sink ->
            sink.asByteWriteChannel().use {
                if (progress != null) channel.copyTo(this, progress)
                else channel.copyTo(this)
            }
        }
    }

    fun read(file: AppFile, progress: (ProgressNotifier)? = null): ByteArray {
        return fs.source(file.absolutePath).use { source ->
            source.buffered().readByteArray()
        }
    }

    fun exists(file: AppFile, progress: (ProgressNotifier)? = null): Boolean {
        return fs.exists(file.absolutePath)
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
        val deletedChildren = if (metadata.isDirectory) {
            fs.list(path).sumOf { child -> deleteRecursively(child, failOnNotFound) }
        } else {
            0
        }
        return deletedChildren + if (tryDelete(path, mustExist = failOnNotFound)) 1 else 0
    }

    /**
     * Deletes [path], swallowing a plain deletion failure (but not "didn't exist" when [mustExist] is true).
     * A directory can go from empty back to non-empty between [deleteRecursively] listing/deleting its children
     * and deleting the directory itself -- e.g. [write] racing [deleteAll] while local data is wiped on logout --
     * which SystemFileSystem surfaces as a bare `IOException("Deletion failed")` rather than something narrower.
     * Losing that one entry isn't worth failing the whole recursive delete over.
     */
    private fun tryDelete(path: Path, mustExist: Boolean): Boolean = try {
        fs.delete(path, mustExist = mustExist)
        true
    } catch (e: IOException) {
        if (mustExist) throw e
        false
    }

    fun deleteAll(): Int {
        var count = deleteRecursively(systemDataPath / DOCUMENTS_PATH, failOnNotFound = false)
        count += deleteRecursively(systemDataPath / IMAGES_PATH, failOnNotFound = false)
        count += deleteRecursively(systemDataPath / FILES_PATH, failOnNotFound = false)
        return count
    }
}
