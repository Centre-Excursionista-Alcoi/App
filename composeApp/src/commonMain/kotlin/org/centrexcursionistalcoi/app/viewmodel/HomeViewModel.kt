package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDate
import org.centrexcursionistalcoi.app.auth.AccountManager
import org.centrexcursionistalcoi.app.data.DatabaseData
import org.centrexcursionistalcoi.app.data.IBookingD
import org.centrexcursionistalcoi.app.data.ItemD
import org.centrexcursionistalcoi.app.data.ItemLendingD
import org.centrexcursionistalcoi.app.data.ItemTypeD
import org.centrexcursionistalcoi.app.data.SectionD
import org.centrexcursionistalcoi.app.data.SpaceBookingD
import org.centrexcursionistalcoi.app.data.SpaceD
import org.centrexcursionistalcoi.app.data.UserD
import org.centrexcursionistalcoi.app.network.InventoryBackend
import org.centrexcursionistalcoi.app.network.SectionsBackend
import org.centrexcursionistalcoi.app.network.SpacesBackend
import org.centrexcursionistalcoi.app.network.UserDataBackend

class HomeViewModel : ViewModel() {
    private val _userData = MutableStateFlow<UserD?>(null)
    val userData get() = _userData.asStateFlow()

    private val _itemBookings = MutableStateFlow<List<ItemLendingD>?>(null)
    val itemBookings get() = _itemBookings.asStateFlow()

    private val _spaceBookings = MutableStateFlow<List<SpaceBookingD>?>(null)
    val spaceBookings get() = _spaceBookings.asStateFlow()


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

    private val _allItemBookings = MutableStateFlow<List<ItemLendingD>?>(null)
    val allItemBookings get() = _allItemBookings.asStateFlow()

    private val _allSpaceBookings = MutableStateFlow<List<SpaceBookingD>?>(null)
    val allSpaceBookings get() = _allSpaceBookings.asStateFlow()

    private val _spaces = MutableStateFlow<List<SpaceD>?>(null)
    val spaces get() = _spaces.asStateFlow()

    private val _creatingSpace = MutableStateFlow(false)
    val creatingSpace get() = _creatingSpace.asStateFlow()

    fun load() {
        launch {
            loadSync()
        }
    }

    private suspend fun loadSync() {
        val data = UserDataBackend.getUserData()
        _userData.emit(data)

        val sections = SectionsBackend.list()
        _sections.emit(sections)

        val types = InventoryBackend.listTypes()
        _itemTypes.emit(types)

        val items = InventoryBackend.listItems()
        _items.emit(items)

        val itemBookings = InventoryBackend.listBookings()
        _itemBookings.emit(itemBookings)

        val spaces = SpacesBackend.list()
        _spaces.emit(spaces)

        val spaceBookings = SpacesBackend.listBookings()
        _spaceBookings.emit(spaceBookings)

        // only for administrators
        if (data.isAdmin) {
            val usersList = UserDataBackend.listUsers()
            _usersList.emit(usersList)

            val allItemBookings = InventoryBackend.allBookings()
            _allItemBookings.emit(allItemBookings)

            val allSpaceBookings = SpacesBackend.allBookings()
            _allSpaceBookings.emit(allSpaceBookings)
        } else {
            _usersList.emit(emptyList())
            _allItemBookings.emit(emptyList())
            _allSpaceBookings.emit(emptyList())
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

            val items = InventoryBackend.availability(from, to)
            _availableItems.emit(items)

            val spaces = SpacesBackend.availability(from, to)
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
                loadSync()
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

    fun cancelBooking(booking: IBookingD, onCancel: () -> Unit) {
        launch {
            try {
                _updatingBooking.emit(true)
                when (booking) {
                    is ItemLendingD -> InventoryBackend.cancelBooking(booking.id!!)
                    is SpaceBookingD -> SpacesBackend.cancelBooking(booking.id!!)
                    else -> error("Unsupported booking type: ${booking::class.simpleName}")
                }
                loadSync()
                uiThread { onCancel() }
            } finally {
                _updatingBooking.emit(false)
            }
        }
    }

    fun confirmBooking(booking: IBookingD, onConfirm: () -> Unit) {
        launch {
            try {
                _updatingBooking.emit(true)
                when (booking) {
                    is ItemLendingD -> InventoryBackend.confirm(booking.id!!)
                    is SpaceBookingD -> SpacesBackend.confirm(booking.id!!)
                    else -> error("Unsupported booking type: ${booking::class.simpleName}")
                }
                loadSync()
                uiThread { onConfirm() }
            } finally {
                _updatingBooking.emit(false)
            }
        }
    }

    fun markAsTaken(booking: IBookingD, meta: Map<String, Any>, onMarked: () -> Unit) {
        launch {
            try {
                _updatingBooking.emit(true)
                when (booking) {
                    is ItemLendingD -> InventoryBackend.markTaken(booking.id!!)
                    is SpaceBookingD -> {
                        val space = spaces.value?.find { it.id == booking.spaceId } ?: error("Space not found")
                        val keys = space.keys?.takeUnless { it.isEmpty() }
                        if (keys != null) {
                            val keyId = meta[BOOKING_CONFIRM_META_SPACE_KEY] as Int
                            SpacesBackend.markTaken(booking.id!!, keyId)
                        } else {
                            SpacesBackend.markTaken(booking.id!!)
                        }
                    }
                    else -> error("Unsupported booking type: ${booking::class.simpleName}")
                }
                loadSync()
                uiThread { onMarked() }
            } finally {
                _updatingBooking.emit(false)
            }
        }
    }

    fun markAsReturned(booking: IBookingD, onMarked: () -> Unit) {
        launch {
            try {
                _updatingBooking.emit(true)
                when (booking) {
                    is ItemLendingD -> InventoryBackend.markReturned(booking.id!!)
                    is SpaceBookingD -> SpacesBackend.markReturned(booking.id!!)
                    else -> error("Unsupported booking type: ${booking::class.simpleName}")
                }
                loadSync()
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
                loadSync()
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
                loadSync()
                uiThread { onDelete() }
            } finally {
                _updatingUser.emit(false)
            }
        }
    }


    companion object {
        /**
         * The key used to pass the space key id when marking a booking as taken.
         * @see markAsTaken
         */
        const val BOOKING_CONFIRM_META_SPACE_KEY = "space_key"
    }
}
