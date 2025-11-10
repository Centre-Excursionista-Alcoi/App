package org.centrexcursionistalcoi.app.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import io.github.aakira.napier.Napier
import io.ktor.utils.io.ByteReadChannel
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.files.SystemPathSeparator
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.network.RemoteRepository
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.storage.fs.FileSystem
import org.centrexcursionistalcoi.app.utils.toUuidOrNull

private fun joinPaths(vararg parts: String): String = parts.joinToString(SystemPathSeparator.toString())

const val DOCUMENTS_PATH = "documents"
const val IMAGES_PATH = "images"
const val FILES_PATH = "files"

/**
 * Returns the file path for the document file associated with this DocumentFileContainer.
 * If the file does not exist locally, it will be downloaded from the remote repository.
 * @param progressNotifier Optional ProgressNotifier to track download progress.
 * @param downloadIfNotExists Whether to download the file if it does not exist locally. If false, path will be returned regardless of existence.
 * @throws IllegalStateException if the document file is not found and uuid cannot be inferred.
 */
suspend fun DocumentFileContainer.fetchDocumentFilePath(progressNotifier: ProgressNotifier? = null, downloadIfNotExists: Boolean = true): String {
    val path = joinPaths(
        DOCUMENTS_PATH,
        this::class.simpleName ?: "generic",
        documentFile?.toString() ?: error("No document file for container")
    )
    if (!FileSystem.exists(path) && downloadIfNotExists) {
        val uuid = path.substringAfterLast(SystemPathSeparator).toUuidOrNull()
            ?: throw IllegalStateException("Document file not found at path ($path). UUID could not be inferred.")
        Napier.d { "Tried to read non-existing file. Downloading..." }
        RemoteRepository.downloadFile(uuid, path, progressNotifier = progressNotifier)
    }
    return path
}

/**
 * Writes the provided ByteReadChannel to the document file associated with this DocumentFileContainer.
 * @throws IllegalStateException if there is no document file associated with the container.
 */
suspend fun DocumentFileContainer.writeFile(channel: ByteReadChannel, progressNotifier: ProgressNotifier? = null) {
    val path = fetchDocumentFilePath(progressNotifier, downloadIfNotExists = false)
    FileSystem.write(path, channel, progressNotifier)
}

suspend fun DocumentFileContainer.readFile(progressNotifier: ProgressNotifier? = null): ByteArray {
    val path = fetchDocumentFilePath(progressNotifier)
    return FileSystem.read(path, progressNotifier)
}

/**
 * Returns the paths for the image file associated with this ImageFileContainer.
 * If the file does not exist locally, it will be downloaded from the remote repository.
 * @param progressNotifier Optional ProgressNotifier to track download progress.
 * @param downloadIfNotExists Whether to download the file if it does not exist locally. If false, path will be returned regardless of existence.
 * @throws IllegalStateException if the image file is not found and uuid cannot be inferred.
 */
suspend fun ImageFileContainer.fetchImageFilePath(progressNotifier: ProgressNotifier? = null, downloadIfNotExists: Boolean = true): String {
    val uuid = image ?: throw IllegalStateException("No image associated with this container.")
    val path = joinPaths(IMAGES_PATH, this::class.simpleName ?: "generic", uuid.toString())
    if (!FileSystem.exists(path) && downloadIfNotExists) {
        val uuid = path.substringAfterLast(SystemPathSeparator).toUuidOrNull()
            ?: throw IllegalStateException("Image file not found at path ($path). UUID could not be inferred.")
        Napier.d { "Tried to read non-existing file. Downloading..." }
        RemoteRepository.downloadFile(uuid, path, progressNotifier = progressNotifier)
    }
    return path
}

suspend fun ImageFileContainer.imageFile(progressNotifier: ProgressNotifier? = null): ByteArray? = image?.let { uuid ->
    val path = fetchImageFilePath(progressNotifier)
    return FileSystem.read(path)
}


/**
 * Returns the paths for all document files associated with this FileContainer.
 */
fun FileContainer.filePaths(): Map<Uuid, String> {
    val clName = this::class.simpleName ?: "generic"
    return files.filter { it.value != null }.map { (_, uuid) ->
        uuid!!
        uuid to joinPaths(FILES_PATH, clName, uuid.toString())
    }.toMap()
}

/**
 * Returns the paths for the file associated with the provided UUID in this FileContainer.
 * If the file does not exist locally, it will be downloaded from the remote repository.
 * @param progressNotifier Optional ProgressNotifier to track download progress.
 * @param downloadIfNotExists Whether to download the file if it does not exist locally. If false, path will be returned regardless of existence.
 * @throws IllegalArgumentException if the file is not found and uuid cannot be inferred.
 */
suspend fun FileContainer.fetchFilePath(uuid: Uuid, progressNotifier: ProgressNotifier? = null, downloadIfNotExists: Boolean = true): String {
    require(files.values.contains(uuid)) { "UUID must be in the container." }

    val path = joinPaths(FILES_PATH, this::class.simpleName ?: "generic", uuid.toString())
    if (!FileSystem.exists(path) && downloadIfNotExists) {
        val uuid = path.substringAfterLast(SystemPathSeparator).toUuidOrNull()
            ?: throw IllegalStateException("Generic file not found at path ($path). UUID could not be inferred.")
        Napier.d { "Tried to read non-existing file. Downloading..." }
        RemoteRepository.downloadFile(uuid, path, progressNotifier = progressNotifier)
    }
    return path
}

/**
 * Writes the provided ByteReadChannel to the document file associated with this FileContainer.
 * @throws IllegalArgumentException if the UUID is not in the container.
 */
suspend fun FileContainer.writeFile(channel: ByteReadChannel, uuid: Uuid, progressNotifier: ProgressNotifier? = null) {
    val path = fetchFilePath(uuid, downloadIfNotExists = false)
    FileSystem.write(path, channel, progressNotifier)
}

/**
 * Reads the file associated with the provided UUID in this FileContainer.
 * @throws IllegalArgumentException if the UUID is not in the container.
 */
suspend fun FileContainer.readFile(uuid: Uuid, progressNotifier: ProgressNotifier? = null): ByteArray {
    val path = fetchFilePath(uuid)
    return FileSystem.read(path, progressNotifier)
}


/**
 * Returns the paths for the file associated with the provided UUID in this SubReferencedFileContainer.
 * If the file does not exist locally, it will be downloaded from the remote repository.
 * @param progressNotifier Optional ProgressNotifier to track download progress.
 * @param downloadIfNotExists Whether to download the file if it does not exist locally. If false, path will be returned regardless of existence.
 * @throws IllegalArgumentException if the file is not found and uuid cannot be inferred.
 */
suspend fun SubReferencedFileContainer.fetchFilePath(uuid: Uuid, progressNotifier: ProgressNotifier? = null, downloadIfNotExists: Boolean = true): String {
    val ref = referencedFiles.find { it.second == uuid }
    require(ref != null) { "UUID must be in the container." }

    val path = joinPaths(FILES_PATH, ref.third, uuid.toString())
    if (!FileSystem.exists(path) && downloadIfNotExists) {
        val uuid = path.substringAfterLast(SystemPathSeparator).toUuidOrNull()
            ?: throw IllegalStateException("Sub-referenced file not found at path ($path). UUID could not be inferred.")
        Napier.d { "Tried to read non-existing file. Downloading..." }
        RemoteRepository.downloadFile(uuid, path, progressNotifier = progressNotifier)
    }
    return path
}

/**
 * Writes the provided ByteReadChannel to the document file associated with this FileContainer.
 * @throws IllegalArgumentException if the UUID is not in the container.
 */
suspend fun SubReferencedFileContainer.writeFile(channel: ByteReadChannel, uuid: Uuid, progressNotifier: ProgressNotifier? = null) {
    val path = fetchFilePath(uuid, progressNotifier, downloadIfNotExists = false)
    FileSystem.write(path, channel, progressNotifier)
}

/**
 * A Composable function that loads the image file associated with this ImageFileContainer.
 *
 * It returns a State<ByteArray?> that will be updated once the image file is loaded.
 *
 * If the image file cannot be loaded, the State will be updated to an empty ByteArray.
 */
@Composable
@OptIn(DelicateCoroutinesApi::class)
fun ImageFileContainer?.rememberImageFile(
    scope: CoroutineScope = GlobalScope,
    dispatcher: CoroutineDispatcher = defaultAsyncDispatcher,
): State<ByteArray?> {
    val state = rememberSaveable { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(this) {
        scope.launch(dispatcher) {
            try {
                val bytes = this@rememberImageFile?.imageFile()
                withContext(Dispatchers.Main) { state.value = bytes }
            } catch (e: IllegalStateException) {
                Napier.w(e) { "Image file not found." }
                withContext(Dispatchers.Main) { state.value = ByteArray(0) }
            }
        }
    }
    return state
}
