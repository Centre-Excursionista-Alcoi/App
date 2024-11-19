package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.centrexcursionistalcoi.app.data.ItemD
import org.centrexcursionistalcoi.app.data.ItemTypeD
import org.centrexcursionistalcoi.app.data.SpaceD
import org.centrexcursionistalcoi.app.data.UserD
import org.centrexcursionistalcoi.app.network.InventoryBackend
import org.centrexcursionistalcoi.app.network.SpacesBackend
import org.centrexcursionistalcoi.app.network.UserDataBackend

class ReservationViewModel : ViewModel() {
    private val _userData = MutableStateFlow<UserD?>(null)
    val userData get() = _userData.asStateFlow()

    private val _items = MutableStateFlow<List<ItemD>?>(null)
    val items get() = _items.asStateFlow()

    private val _types = MutableStateFlow<List<ItemTypeD>?>(null)
    val types get() = _types.asStateFlow()

    private val _space = MutableStateFlow<SpaceD?>(null)
    val space get() = _space.asStateFlow()

    fun load(filterItemIds: Set<Int>, selectedSpaceId: Int?) {
        launch {
            val data = UserDataBackend.getUserData()
            _userData.emit(data)

            if (filterItemIds.isNotEmpty()) {
                val items = InventoryBackend.listItems(filterItemIds)
                _items.emit(items)

                val types = InventoryBackend.listTypes()
                _types.emit(types)
            } else {
                _items.emit(emptyList())
                _types.emit(emptyList())
            }

            if (selectedSpaceId != null) {
                val space = SpacesBackend.get(selectedSpaceId)
                _space.emit(space)
            }
        }
    }

    fun confirm(from: LocalDate, to: LocalDate, itemIds: Set<Int>, spaceId: Int?, onBookingComplete: () -> Unit) {
        launch {
            if (itemIds.isNotEmpty()) {
                InventoryBackend.book(
                    from = from.atStartOfDayIn(TimeZone.currentSystemDefault()),
                    to = to.atStartOfDayIn(TimeZone.currentSystemDefault()),
                    itemIds = itemIds
                )
            }
            if (spaceId != null) {
                // TODO: Book space
            }
            uiThread { onBookingComplete() }
        }
    }
}
