package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.centrexcursionistalcoi.app.auth.AccountManager
import org.centrexcursionistalcoi.app.data.DatabaseData
import org.centrexcursionistalcoi.app.data.ItemD
import org.centrexcursionistalcoi.app.data.ItemLendingD
import org.centrexcursionistalcoi.app.data.ItemTypeD
import org.centrexcursionistalcoi.app.data.SectionD
import org.centrexcursionistalcoi.app.data.SpaceD
import org.centrexcursionistalcoi.app.data.UserD
import org.centrexcursionistalcoi.app.network.InventoryBackend
import org.centrexcursionistalcoi.app.network.SectionsBackend
import org.centrexcursionistalcoi.app.network.SpacesBackend
import org.centrexcursionistalcoi.app.network.UserDataBackend
import org.centrexcursionistalcoi.app.utils.atEndOfDayInMilliseconds
import org.centrexcursionistalcoi.app.utils.atStartOfDayInMilliseconds

class HomeViewModel : ViewModel() {
    private val _userData = MutableStateFlow<UserD?>(null)
    val userData get() = _userData.asStateFlow()

    private val _bookings = MutableStateFlow<List<ItemLendingD>?>(null)
    val bookings get() = _bookings.asStateFlow()


    private val _availableItems = MutableStateFlow<List<ItemD>?>(null)
    val availableItems get() = _availableItems.asStateFlow()

    private val _availableSpaces = MutableStateFlow<List<SpaceD>?>(null)
    val availableSpaces get() = _availableSpaces.asStateFlow()


    private val _usersList = MutableStateFlow<List<UserD>?>(null)
    val usersList get() = _usersList.asStateFlow()

    private val _updatingUser = MutableStateFlow(false)
    val updatingUser get() = _updatingUser.asStateFlow()

    private val _sections = MutableStateFlow<List<SectionD>?>(null)
    val sections get() = _sections.asStateFlow()

    private val _creatingSection = MutableStateFlow(false)
    val creatingSection get() = _creatingSection.asStateFlow()

    private val _itemTypes = MutableStateFlow<List<ItemTypeD>?>(null)
    val itemTypes get() = _itemTypes.asStateFlow()

    private val _creatingType = MutableStateFlow(false)
    val creatingType get() = _creatingType.asStateFlow()

    private val _items = MutableStateFlow<List<ItemD>?>(null)
    val items get() = _items.asStateFlow()

    private val _creatingItem = MutableStateFlow(false)
    val creatingItem get() = _creatingItem.asStateFlow()

    private val _updatingBooking = MutableStateFlow(false)
    val updatingBooking get() = _updatingBooking.asStateFlow()

    private val _allBookings = MutableStateFlow<List<ItemLendingD>?>(null)
    val allBookings get() = _allBookings.asStateFlow()

    private val _spaces = MutableStateFlow<List<SpaceD>?>(null)
    val spaces get() = _spaces.asStateFlow()

    private val _creatingSpace = MutableStateFlow(false)
    val creatingSpace get() = _creatingSpace.asStateFlow()

    fun load() {
        launch {
            val data = UserDataBackend.getUserData()
            _userData.emit(data)

            val sections = SectionsBackend.list()
            _sections.emit(sections)

            val types = InventoryBackend.listTypes()
            _itemTypes.emit(types)

            val items = InventoryBackend.listItems()
            _items.emit(items)

            val bookings = InventoryBackend.listBookings()
            _bookings.emit(bookings)

            val allBookings = InventoryBackend.allBookings()
            _allBookings.emit(allBookings)

            val usersList = UserDataBackend.listUsers()
            _usersList.emit(usersList)

            val spaces = SpacesBackend.list()
            _spaces.emit(spaces)
        }
    }

    fun logout() {
        launch {
            AccountManager.logout()
        }
    }

    fun availability(from: LocalDate, to: LocalDate) {
        launch {
            _availableItems.emit(null)
            _availableSpaces.emit(null)

            val items = InventoryBackend.availability(
                from.atStartOfDayInMilliseconds(TimeZone.currentSystemDefault()),
                to.atEndOfDayInMilliseconds(TimeZone.currentSystemDefault())
            )
            _availableItems.emit(items)

            val spaces = SpacesBackend.availability(
                from.atStartOfDayInMilliseconds(TimeZone.currentSystemDefault()),
                to.atEndOfDayInMilliseconds(TimeZone.currentSystemDefault())
            )
            _availableSpaces.emit(spaces)
        }
    }

    private fun <Type: DatabaseData> onCreateOrUpdate(
        value: Type,
        creating: MutableStateFlow<Boolean>,
        creator: suspend (Type) -> Unit,
        updater: suspend (Type) -> Unit,
        onCreate: () -> Unit
    ) {
        launch {
            try {
                creating.emit(true)
                if (value.id == null) {
                    creator(value)
                } else {
                    updater(value)
                }
                load()
                uiThread { onCreate() }
            } finally {
                creating.emit(false)
            }
        }
    }

    fun onCreateOrUpdate(sectionD: SectionD, onCreate: () -> Unit) {
        onCreateOrUpdate(
            sectionD,
            _creatingSection,
            SectionsBackend::create,
            SectionsBackend::update,
            onCreate
        )
    }

    fun createOrUpdate(itemTypeD: ItemTypeD, onCreate: () -> Unit) {
        onCreateOrUpdate(
            itemTypeD,
            _creatingType,
            InventoryBackend::create,
            InventoryBackend::update,
            onCreate
        )
    }

    fun createOrUpdate(itemD: ItemD, onCreate: () -> Unit) {
        onCreateOrUpdate(
            itemD,
            _creatingItem,
            InventoryBackend::create,
            InventoryBackend::update,
            onCreate
        )
    }

    fun createOrUpdate(spaceD: SpaceD, onCreate: () -> Unit) {
        onCreateOrUpdate(
            spaceD,
            _creatingSpace,
            SpacesBackend::create,
            SpacesBackend::update,
            onCreate
        )
    }

    fun confirmBooking(booking: ItemLendingD, onConfirm: () -> Unit) {
        launch {
            try {
                _updatingBooking.emit(true)
                InventoryBackend.confirm(booking.id!!)
                load()
                uiThread { onConfirm() }
            } finally {
                _updatingBooking.emit(false)
            }
        }
    }

    fun markAsTaken(booking: ItemLendingD, onMarked: () -> Unit) {
        launch {
            try {
                _updatingBooking.emit(true)
                InventoryBackend.markTaken(booking.id!!)
                load()
                uiThread { onMarked() }
            } finally {
                _updatingBooking.emit(false)
            }
        }
    }

    fun markAsReturned(booking: ItemLendingD, onMarked: () -> Unit) {
        launch {
            try {
                _updatingBooking.emit(true)
                InventoryBackend.markReturned(booking.id!!)
                load()
                uiThread { onMarked() }
            } finally {
                _updatingBooking.emit(false)
            }
        }
    }

    fun confirm(user: UserD, onConfirm: () -> Unit) {
        launch {
            try {
                _updatingUser.emit(true)
                UserDataBackend.confirm(user)
                load()
                uiThread { onConfirm() }
            } finally {
                _updatingUser.emit(false)
            }
        }
    }

    fun delete(user: UserD, onDelete: () -> Unit) {
        launch {
            try {
                _updatingUser.emit(true)
                UserDataBackend.delete(user)
                load()
                uiThread { onDelete() }
            } finally {
                _updatingUser.emit(false)
            }
        }
    }
}
