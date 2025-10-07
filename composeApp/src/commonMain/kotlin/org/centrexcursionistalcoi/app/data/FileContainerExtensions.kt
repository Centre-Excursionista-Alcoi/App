package org.centrexcursionistalcoi.app.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.storage.fs.PlatformFileSystem

suspend fun ImageFileContainer.imageFile(): ByteArray? = image?.let {
    val clName = this::class.simpleName
    PlatformFileSystem.read("image$clName$it")
}

@Composable
@OptIn(DelicateCoroutinesApi::class)
fun ImageFileContainer.rememberImageFile(): State<ByteArray?> {
    val state = remember { mutableStateOf<ByteArray?>(null) }
    GlobalScope.launch(defaultAsyncDispatcher) {
        val bytes = imageFile()
        withContext(Dispatchers.Main) { state.value = bytes }
    }
    return state
}
