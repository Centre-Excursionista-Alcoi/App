package org.centrexcursionistalcoi.app.viewmodel.admin

import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import org.centrexcursionistalcoi.app.database.appDatabase
import org.centrexcursionistalcoi.app.database.entity.ItemType
import org.centrexcursionistalcoi.app.network.InventoryBackend
import org.centrexcursionistalcoi.app.viewmodel.launch

class ItemTypeEditorViewModel : AdminViewModel() {
    private val inventoryDao = appDatabase.inventoryDao()

    private val _itemType = MutableStateFlow<ItemType?>(null)
    val itemType get() = _itemType.asStateFlow()

    val sections = inventoryDao
        .getAllSectionsAsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading get() = _isLoading.asStateFlow()

    fun load(itemTypeId: Int?) {
        launch {
            if (itemTypeId != null) {
                val itemType = inventoryDao.getItemType(itemTypeId)
                _itemType.emit(itemType)
            }
        }
    }

    fun setItemType(itemType: ItemType) {
        _itemType.tryEmit(itemType)
    }

    fun createOrUpdate(onCreate: () -> Unit) {
        val itemType = itemType.value
        if (itemType == null) {
            Napier.w { "Item type is null." }
            return
        }
        onCreateOrUpdate(
            itemType,
            _isLoading,
            InventoryBackend::create,
            InventoryBackend::update,
            onCreate
        )
    }
}
