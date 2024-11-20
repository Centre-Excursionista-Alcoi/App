package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDate
import org.centrexcursionistalcoi.app.data.ItemD
import org.centrexcursionistalcoi.app.data.ItemLendingD
import org.centrexcursionistalcoi.app.data.ItemTypeD
import org.centrexcursionistalcoi.app.data.SpaceBookingD
import org.centrexcursionistalcoi.app.data.SpaceD
import org.centrexcursionistalcoi.app.data.UserD
import org.centrexcursionistalcoi.app.network.InventoryBackend
import org.centrexcursionistalcoi.app.network.SpacesBackend
import org.centrexcursionistalcoi.app.network.UserDataBackend

class ReservationViewModel : ViewModel() {
    private val _userData = MutableStateFlow<UserD?>(null)
    val userData get() = _userData.asStateFlow()

    private val _dates = MutableStateFlow<ClosedRange<LocalDate>?>(null)
    val dates get() = _dates.asStateFlow()

    private val _items = MutableStateFlow<List<ItemD>?>(null)
    val items get() = _items.asStateFlow()

    private val _types = MutableStateFlow<List<ItemTypeD>?>(null)
    val types get() = _types.asStateFlow()

    private val _space = MutableStateFlow<SpaceD?>(null)
    val space get() = _space.asStateFlow()


    private val _itemLending = MutableStateFlow<ItemLendingD?>(null)
    val itemLending get() = _itemLending.asStateFlow()

    private val _spaceBooking = MutableStateFlow<SpaceBookingD?>(null)
    val spaceBooking get() = _spaceBooking.asStateFlow()

    private var itemLendingId: Int? = null
    private var spaceLendingId: Int? = null


    fun load(
        from: LocalDate,
        to: LocalDate,
        filterItemIds: Set<Int>,
        selectedSpaceId: Int?
    ) {
        launch {
            _dates.emit(from..to)

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

    fun load(itemLendingId: Int?, spaceBookingId: Int?) {
        launch {
            this.itemLendingId = itemLendingId
            this.spaceLendingId = spaceBookingId

            val data = UserDataBackend.getUserData()
            _userData.emit(data)

            var from: LocalDate? = null
            var to: LocalDate? = null

            if (itemLendingId != null) {
                val itemLending = InventoryBackend.getBooking(itemLendingId)
                _itemLending.emit(itemLending)

                from = itemLending.from
                to = itemLending.to

                val types = InventoryBackend.listTypes()
                _types.emit(types)

                itemLending.itemIds?.let { itemIds ->
                    val items = InventoryBackend.listItems(itemIds)
                    _items.emit(items)
                } ?: run {
                    _items.emit(emptyList())
                }
            } else {
                _items.emit(emptyList())
                _types.emit(emptyList())
            }

            if (spaceBookingId != null) {
                val spaceBooking = SpacesBackend.getBooking(spaceBookingId)
                _spaceBooking.emit(spaceBooking)

                val sbFrom = spaceBooking.from
                val sbTo = spaceBooking.to
                from = if (from == null) sbFrom else if (sbFrom != null) minOf(from, sbFrom) else null
                to = if (to == null) sbTo else if (sbTo != null) maxOf(to, sbTo) else null

                spaceBooking.spaceId?.let { spaceId ->
                    val space = SpacesBackend.get(spaceId)
                    _space.emit(space)
                }
            }

            if (from == null || to == null) {
                error("No dates found")
            }

            _dates.emit(from..to)
        }
    }

    fun confirm(from: LocalDate, to: LocalDate, itemIds: Set<Int>, spaceId: Int?, onBookingComplete: () -> Unit) {
        launch {
            if (itemIds.isNotEmpty()) {
                InventoryBackend.book(from, to, itemIds)
            }
            if (spaceId != null) {
                SpacesBackend.book(spaceId, from, to)
            }
            uiThread { onBookingComplete() }
        }
    }

    fun cancelBooking(onComplete: () -> Unit) {
        launch {
            itemLendingId?.let { InventoryBackend.cancelBooking(it) }
            spaceLendingId?.let { SpacesBackend.cancelBooking(it) }
            uiThread { onComplete() }
        }
    }
}
