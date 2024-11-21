package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import com.russhwolf.settings.ExperimentalSettingsApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import org.centrexcursionistalcoi.app.data.UserD
import org.centrexcursionistalcoi.app.database.appDatabase
import org.centrexcursionistalcoi.app.database.entity.Item
import org.centrexcursionistalcoi.app.database.entity.ItemBooking
import org.centrexcursionistalcoi.app.database.entity.Space
import org.centrexcursionistalcoi.app.database.entity.SpaceBooking
import org.centrexcursionistalcoi.app.network.InventoryBackend
import org.centrexcursionistalcoi.app.network.SpacesBackend
import org.centrexcursionistalcoi.app.serverJson
import org.centrexcursionistalcoi.app.settings.SettingsKeys
import org.centrexcursionistalcoi.app.settings.settings

@OptIn(ExperimentalSettingsApi::class)
class ReservationViewModel : ViewModel() {
    private val bookingsDao = appDatabase.bookingsDao()
    private val inventoryDao = appDatabase.inventoryDao()
    private val spacesDao = appDatabase.spacesDao()

    val userData
        get() = settings.getStringOrNullFlow(SettingsKeys.USER_DATA)
            .map { json -> json?.let { serverJson.decodeFromString(UserD.serializer(), it) } }

    private val _dates = MutableStateFlow<ClosedRange<LocalDate>?>(null)
    val dates get() = _dates.asStateFlow()

    private val _items = MutableStateFlow<List<Item>?>(null)
    val items get() = _items.asStateFlow()

    val types get() = inventoryDao.getAllItemTypesAsFlow()

    private val _space = MutableStateFlow<Space?>(null)
    val space get() = _space.asStateFlow()


    private val _itemBooking = MutableStateFlow<ItemBooking?>(null)
    val itemBooking get() = _itemBooking.asStateFlow()

    private val _spaceBooking = MutableStateFlow<SpaceBooking?>(null)
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

            if (filterItemIds.isNotEmpty()) {
                val items = inventoryDao.getAllItemsFromIds(filterItemIds.toList())
                _items.emit(items)
            } else {
                _items.emit(emptyList())
            }

            if (selectedSpaceId != null) {
                val space = spacesDao.getSpaceById(selectedSpaceId)
                _space.emit(space)
            }
        }
    }

    fun load(itemLendingId: Int?, spaceBookingId: Int?) {
        launch {
            this.itemLendingId = itemLendingId
            this.spaceLendingId = spaceBookingId

            var from: LocalDate? = null
            var to: LocalDate? = null

            if (itemLendingId != null) {
                val itemLending = bookingsDao.getItemBookingWithId(itemLendingId)
                _itemBooking.emit(itemLending)

                from = itemLending?.from
                to = itemLending?.to

                itemLending?.itemIds?.let { itemIds ->
                    val items = inventoryDao.getAllItemsFromIds(itemIds.toList())
                    _items.emit(items)
                }
            } else {
                _items.emit(emptyList())
            }

            if (spaceBookingId != null) {
                val spaceBooking = bookingsDao.getSpaceBookingWithId(spaceBookingId)
                _spaceBooking.emit(spaceBooking)

                val sbFrom = spaceBooking?.from
                val sbTo = spaceBooking?.to
                from = if (from == null) sbFrom else if (sbFrom != null) minOf(from, sbFrom) else null
                to = if (to == null) sbTo else if (sbTo != null) maxOf(to, sbTo) else null

                spaceBooking?.spaceId?.let { spaceId ->
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
