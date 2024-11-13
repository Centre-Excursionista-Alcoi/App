package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.centrexcursionistalcoi.app.network.InventoryBackend
import org.centrexcursionistalcoi.app.network.UserDataBackend
import org.centrexcursionistalcoi.app.server.response.data.ItemD
import org.centrexcursionistalcoi.app.server.response.data.ItemTypeD
import org.centrexcursionistalcoi.app.server.response.data.UserD

class ReservationViewModel : ViewModel() {
    private val _userData = MutableStateFlow<UserD?>(null)
    val userData get() = _userData.asStateFlow()

    private val _items = MutableStateFlow<List<ItemD>?>(null)
    val items get() = _items.asStateFlow()

    private val _types = MutableStateFlow<List<ItemTypeD>?>(null)
    val types get() = _types.asStateFlow()

    fun load(filterItemIds: Set<Int>) {
        launch {
            val data = UserDataBackend.getUserData()
            _userData.emit(data)

            val items = InventoryBackend.listItems(filterItemIds)
            _items.emit(items)

            val types = InventoryBackend.listTypes()
            _types.emit(types)
        }
    }

    fun confirm(from: LocalDate, to: LocalDate, itemIds: Set<Int>, onBookingComplete: () -> Unit) {
        launch {
            InventoryBackend.book(
                from = from.atStartOfDayIn(TimeZone.currentSystemDefault()),
                to = to.atStartOfDayIn(TimeZone.currentSystemDefault()),
                itemIds = itemIds
            )
            uiThread { onBookingComplete() }
        }
    }
}
