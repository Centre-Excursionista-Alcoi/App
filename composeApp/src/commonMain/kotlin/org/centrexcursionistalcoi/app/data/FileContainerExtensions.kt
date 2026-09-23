package org.centrexcursionistalcoi.app.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.diamondedge.logging.logging
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.files.SystemPathSeparator
import org.centrexcursionistalcoi.app.di.DispatcherProvider
import org.centrexcursionistalcoi.app.network.RemoteRepository
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.storage.fs.AppFile
import org.centrexcursionistalcoi.app.storage.fs.FileSystem
import org.centrexcursionistalcoi.app.storage.fs.read
import org.centrexcursionistalcoi.app.storage.fs.write
import org.centrexcursionistalcoi.app.utils.toUuidOrNull
import org.koin.compose.koinInject
import kotlin.uuid.Uuid

private val log = logging()

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
suspend fun DocumentFileContainer.fetchDocumentFilePath(progressNotifier: ProgressNotifier? = null, downloadIfNotExists: Boolean = true): AppFile {
    val path = joinPaths(
        DOCUMENTS_PATH,
        this::class.simpleName ?: "generic",
        documentFile?.toString() ?: error("No document file for container")
    )
    if (!FileSystem.exists(path) && downloadIfNotExists) {
        val uuid = path.substringAfterLast(SystemPathSeparator).toUuidOrNull()
            ?: throw IllegalStateException("Document file not found at path ($path). UUID could not be inferred.")
        log.d { "Tried to read non-existing file. Downloading..." }
        RemoteRepository.downloadFile(uuid, path, progressNotifier = progressNotifier)
    }
    return AppFile(path)
}

/**
 * Writes the provided ByteReadChannel to the document file associated with this DocumentFileContainer.
 * @throws IllegalStateException if there is no document file associated with the container.
 */
suspend fun DocumentFileContainer.writeFile(channel: ByteReadChannel, progressNotifier: ProgressNotifier? = null) {
    val file = fetchDocumentFilePath(progressNotifier, downloadIfNotExists = false)
    file.write(channel, progressNotifier)
}

suspend fun DocumentFileContainer.readFile(progressNotifier: ProgressNotifier? = null): ByteArray {
    val file = fetchDocumentFilePath(progressNotifier)
    return file.read(progressNotifier)
}

/**
 * Returns the paths for the image file with the given UUID.
 * If the file does not exist locally, it will be downloaded from the remote repository.
 * @param uuid The UUID of the file to fetch.
 * @param className The class name of the calling class, `null` if unknown.
 * @param progressNotifier Optional ProgressNotifier to track download progress.
 * @param downloadIfNotExists Whether to download the file if it does not exist locally. If false, path will be returned regardless of existence.
 */
private suspend fun fetchImageFilePath(
    uuid: Uuid,
    className: String?,
    progressNotifier: ProgressNotifier? = null,
    downloadIfNotExists: Boolean = true
): AppFile {
    val path = joinPaths(IMAGES_PATH, className ?: "generic", uuid.toString())
    if (!FileSystem.exists(path) && downloadIfNotExists) {
        val uuid = path.substringAfterLast(SystemPathSeparator).toUuidOrNull()
            ?: throw IllegalStateException("Image file not found at path ($path). UUID could not be inferred.")
        log.d { "Tried to read non-existing file. Downloading..." }
        RemoteRepository.downloadFile(uuid, path, progressNotifier = progressNotifier)
    }
    return AppFile(path)
}

/**
 * Returns the paths for the image file associated with this ImageFileContainer.
 * If the file does not exist locally, it will be downloaded from the remote repository.
 * @param progressNotifier Optional ProgressNotifier to track download progress.
 * @param downloadIfNotExists Whether to download the file if it does not exist locally. If false, path will be returned regardless of existence.
 * @throws IllegalStateException if the image file is not found and uuid cannot be inferred.
 */
suspend fun ImageFileContainer.fetchImageFilePath(progressNotifier: ProgressNotifier? = null, downloadIfNotExists: Boolean = true): AppFile {
    val uuid = image ?: throw IllegalStateException("No image associated with this container.")
    return fetchImageFilePath(uuid, this::class.simpleName, progressNotifier, downloadIfNotExists)
}

suspend fun ImageFileContainer.imageFile(progressNotifier: ProgressNotifier? = null): ByteArray? = image?.let {
    val file = fetchImageFilePath(progressNotifier)
    return file.read()
}

suspend fun ImageFileListContainer.imageFile(uuid: Uuid, progressNotifier: ProgressNotifier? = null, downloadIfNotExists: Boolean = true): ByteArray? {
    if (!images.contains(uuid)) throw IllegalArgumentException("Could not find image $uuid in container")
    val file = fetchImageFilePath(uuid, this::class.simpleName, progressNotifier, downloadIfNotExists)
    return file.read()
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
    dispatcherProvider: DispatcherProvider = koinInject(),
): State<ByteArray?> {
    val state = remember { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(this) {
        scope.launch(dispatcherProvider.io) {
            try {
                val bytes = this@rememberImageFile?.imageFile()
                withContext(dispatcherProvider.main) { state.value = bytes }
            } catch (e: IllegalStateException) {
                log.w(e) { "Image file not found." }
                withContext(dispatcherProvider.main) { state.value = ByteArray(0) }
            }
        }
    }
    return state
}

/**
 * Loads the image files of this container asynchronously.
 * @param scope The scope to use for launching the coroutines that load the images.
 * @param dispatcher The dispatcher to use for the coroutines.
 * @return An [SnapshotStateMap] that holds the UUIDs of the images as keys, and their loaded data as values.
 * When loading the images, values will be null. Once loaded, if the array is empty, the image was not found, or could not be loaded.
 */
@Composable
@OptIn(DelicateCoroutinesApi::class)
fun ImageFileListContainer?.rememberImageFiles(
    scope: CoroutineScope = GlobalScope,
    dispatcherProvider: DispatcherProvider = koinInject(),
): SnapshotStateMap<Uuid, ByteArray?> {
    val state = remember { mutableStateMapOf<Uuid, ByteArray?>() }
    LaunchedEffect(this) {
        if (this@rememberImageFiles == null) return@LaunchedEffect
        for (image in images) {
            scope.launch(dispatcherProvider.io) {
                withContext(dispatcherProvider.main) { state[image] = null }
                try {
                    val bytes = imageFile(uuid = image)
                    withContext(dispatcherProvider.main) { state[image] = bytes }
                } catch (e: IllegalArgumentException) {
                    log.w(e) { "Image file not found." }
                    withContext(dispatcherProvider.main) { state[image] = ByteArray(0) }
                }
            }
        }
    }
    return state
}