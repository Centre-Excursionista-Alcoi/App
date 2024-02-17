package screenmodel

import androidx.compose.ui.graphics.ImageBitmap
import backend.StockManagement
import backend.data.database.InventoryItem
import backend.wrapper.SupabaseWrapper
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.io.Buffer
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.readByteArray
import platform.ImageProcessor
import storage.AppFileSystem

class InventoryItemScreenModel : ScreenModel {
    val images = MutableStateFlow<Map<InventoryItem, Map<String, ImageBitmap>>>(emptyMap())

    @OptIn(ExperimentalStdlibApi::class)
    fun loadImage(item: InventoryItem, path: String) = screenModelScope.launch(Dispatchers.IO) {
        val dir = Path(AppFileSystem.root, "inventory_item", "images").also {
            AppFileSystem.fileSystem.createDirectories(it)
        }
        val file = Path(dir, path)
        val bytes = if (AppFileSystem.fileSystem.exists(file)) {
            AppFileSystem.fileSystem.source(file).buffered().readByteArray()
        } else {
            Napier.i(tag = "item-${item.id}") { "Downloading image from storage bucket ($path)..." }
            SupabaseWrapper.storage
                .from("item_images")
                .downloadAuthenticated(path)
                .also { data ->
                    Napier.d(tag = "item-${item.id}") { "Image downloaded, copying to storage ($file)..." }
                    val buffer = Buffer()
                    buffer.use { it.write(data) }
                    AppFileSystem.fileSystem.sink(file).write(buffer, buffer.size)
                    Napier.i(tag = "item-${item.id}") { "Image downloaded successfully" }
                }
        }
        val bitmap = ImageProcessor.processImage(bytes)
        images.value = images.value.toMutableMap().also { it[item] = it[item].orEmpty() + (path to bitmap) }
    }

    fun loadAvailableStock() = screenModelScope.launch(Dispatchers.IO) {
        StockManagement.loadAvailableStock()
    }
}
