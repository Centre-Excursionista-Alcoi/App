package org.centrexcursionistalcoi.app.viewmodel.admin

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.centrexcursionistalcoi.app.database.appDatabase
import org.centrexcursionistalcoi.app.database.entity.Item
import org.centrexcursionistalcoi.app.database.entity.ItemType
import org.centrexcursionistalcoi.app.network.InventoryBackend
import org.centrexcursionistalcoi.app.viewmodel.launch

class ItemTypeViewModel: AdminViewModel() {
    private val bookingsDao = appDatabase.bookingsDao()
    private val inventoryDao = appDatabase.inventoryDao()

    val allBookings = bookingsDao.getAllItemBookingsAsFlow()

    val itemTypes = inventoryDao.getAllItemTypesAsFlow()

    private val _itemType = MutableStateFlow<ItemType?>(null)
    val itemType = _itemType.asStateFlow()

    private val _items = MutableStateFlow<List<Item>?>(null)
    val items = _items.asStateFlow()


    private val _creatingItem = MutableStateFlow(false)
    val creatingItem get() = _creatingItem.asStateFlow()

    fun createOrUpdate(item: Item, onCreate: () -> Unit) {
        onCreateOrUpdate(
            item,
            _creatingItem,
            InventoryBackend::create,
            InventoryBackend::update,
            onCreate
        )
    }

    fun load(itemTypeId: Int) {
        launch {
            val itemType = inventoryDao.getItemType(itemTypeId)
            _itemType.emit(itemType)

            val items = inventoryDao.getAllItemsFromItemType(itemTypeId)
            _items.emit(items)
        }
    }
}
