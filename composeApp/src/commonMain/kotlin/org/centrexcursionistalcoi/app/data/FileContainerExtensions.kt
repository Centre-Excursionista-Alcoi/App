package org.centrexcursionistalcoi.app.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import org.centrexcursionistalcoi.app.storage.fs.PlatformFileSystem

private fun joinPaths(vararg parts: String): String = parts.joinToString(SystemPathSeparator.toString())

fun DocumentFileContainer.documentFilePath(uuid: Uuid): String {
    val clName = this::class.simpleName ?: "generic"
    return joinPaths("document", clName, uuid.toString())
}

suspend fun DocumentFileContainer.writeFile(uuid: Uuid, channel: ByteReadChannel) {
    val path = documentFilePath(uuid)
    PlatformFileSystem.write(path, channel)
}

suspend fun DocumentFileContainer.readFile(uuid: Uuid): ByteArray {
    val path = documentFilePath(uuid)
    return PlatformFileSystem.read(path)
}

private fun ImageFileContainer.imageFilePath(uuid: Uuid): String {
    val clName = this::class.simpleName ?: "generic"
    return joinPaths("image", clName, uuid.toString())
}

suspend fun ImageFileContainer.writeImageFile(uuid: Uuid, channel: ByteReadChannel) {
    val path = imageFilePath(uuid)
    PlatformFileSystem.write(path, channel)
}

suspend fun ImageFileContainer.imageFile(): ByteArray? = image?.let { uuid ->
    val path = imageFilePath(uuid)
    if (!PlatformFileSystem.exists(path)) {
        throw IllegalStateException("Image file not found at path: $path")
    }
    return PlatformFileSystem.read(path)
}

/**
 * A Composable function that loads the image file associated with this ImageFileContainer.
 *
 * It returns a State<ByteArray?> that will be updated once the image file is loaded.
 *
 * If the image file does not exist, the State will be updated to an empty ByteArray.
 */
@Composable
@OptIn(DelicateCoroutinesApi::class)
fun ImageFileContainer.rememberImageFile(
    scope: CoroutineScope = GlobalScope,
    dispatcher: CoroutineDispatcher = defaultAsyncDispatcher,
): State<ByteArray?> {
    val state = remember { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(Unit) {
        scope.launch(dispatcher) {
            try {
                val bytes = imageFile()
                withContext(Dispatchers.Main) { state.value = bytes }
            } catch (e: IllegalStateException) {
                Napier.w(e) { "Image file not found." }
                withContext(Dispatchers.Main) { state.value = ByteArray(0) }
            }
        }
    }
    return state
}
