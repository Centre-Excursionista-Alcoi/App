package org.centrexcursionistalcoi.app.viewmodel

import com.russhwolf.settings.ExperimentalSettingsApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import org.centrexcursionistalcoi.app.auth.AccountManager
import org.centrexcursionistalcoi.app.data.UserD
import org.centrexcursionistalcoi.app.database.appDatabase
import org.centrexcursionistalcoi.app.database.entity.BookingEntity
import org.centrexcursionistalcoi.app.database.entity.Item
import org.centrexcursionistalcoi.app.database.entity.ItemBooking
import org.centrexcursionistalcoi.app.database.entity.ItemType
import org.centrexcursionistalcoi.app.database.entity.Section
import org.centrexcursionistalcoi.app.database.entity.Space
import org.centrexcursionistalcoi.app.database.entity.SpaceBooking
import org.centrexcursionistalcoi.app.database.entity.admin.User
import org.centrexcursionistalcoi.app.network.InventoryBackend
import org.centrexcursionistalcoi.app.network.SectionsBackend
import org.centrexcursionistalcoi.app.network.SpacesBackend
import org.centrexcursionistalcoi.app.network.Sync
import org.centrexcursionistalcoi.app.network.UserDataBackend
import org.centrexcursionistalcoi.app.serverJson
import org.centrexcursionistalcoi.app.settings.SettingsKeys
import org.centrexcursionistalcoi.app.settings.settings
import org.centrexcursionistalcoi.app.viewmodel.admin.AdminViewModel

@OptIn(ExperimentalSettingsApi::class)
class HomeViewModel : AdminViewModel() {
    private val bookingsDao = appDatabase.bookingsDao()
    private val inventoryDao = appDatabase.inventoryDao()
    private val spacesDao = appDatabase.spacesDao()
    private val adminDao = appDatabase.adminDao()

    val userData
        get() = settings.getStringOrNullFlow(SettingsKeys.USER_DATA)
            .map { json -> json?.let { serverJson.decodeFromString(UserD.serializer(), it) } }

    val itemBookings
        get() = combine(userData, bookingsDao.getAllItemBookingsAsFlow()) { user, bookings ->
            bookings.filter { it.userId == user?.email }
        }
    val spaceBookings
        get() = combine(userData, bookingsDao.getAllSpaceBookingsAsFlow()) { user, bookings ->
            bookings.filter { it.userId == user?.email }
        }


    private val _availableItems = MutableStateFlow<List<Item>?>(null)
    val availableItems get() = _availableItems.asStateFlow()

    private val _availableSpaces = MutableStateFlow<List<Space>?>(null)
    val availableSpaces get() = _availableSpaces.asStateFlow()


    val usersList get() = adminDao.getAllUsersAsFlow()

    private val _updatingUser = MutableStateFlow(false)
    val updatingUser get() = _updatingUser.asStateFlow()

    val sections get() = inventoryDao.getAllSectionsAsFlow()

    private val _creatingSection = MutableStateFlow(false)
    val creatingSection get() = _creatingSection.asStateFlow()

    val itemTypes get() = inventoryDao.getAllItemTypesAsFlow()

    private val _creatingType = MutableStateFlow(false)
    val creatingType get() = _creatingType.asStateFlow()

    val items get() = inventoryDao.getAllItemsAsFlow()

    private val _updatingBooking = MutableStateFlow(false)
    val updatingBooking get() = _updatingBooking.asStateFlow()

    val allItemBookings get() = bookingsDao.getAllItemBookingsAsFlow()
    val allSpaceBookings get() = bookingsDao.getAllSpaceBookingsAsFlow()

    val spaces get() = spacesDao.getAllSpacesAsFlow()

    private val _creatingSpace = MutableStateFlow(false)
    val creatingSpace get() = _creatingSpace.asStateFlow()

    fun load() {
        launch {
            Sync.syncBookings()
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

    fun onCreateOrUpdate(section: Section, onCreate: () -> Unit) {
        onCreateOrUpdate(
            section,
            _creatingSection,
            SectionsBackend::create,
            SectionsBackend::update,
            onCreate
        )
    }

    fun createOrUpdate(itemType: ItemType, onCreate: () -> Unit) {
        onCreateOrUpdate(
            itemType,
            _creatingType,
            InventoryBackend::create,
            InventoryBackend::update,
            onCreate
        )
    }

    fun createOrUpdate(space: Space, onCreate: () -> Unit) {
        onCreateOrUpdate(
            space,
            _creatingSpace,
            SpacesBackend::create,
            SpacesBackend::update,
            onCreate
        )
    }

    fun cancelBooking(booking: BookingEntity<*>, onCancel: () -> Unit) {
        launch {
            try {
                _updatingBooking.emit(true)
                when (booking) {
                    is ItemBooking -> InventoryBackend.cancelBooking(booking.id)
                    is SpaceBooking -> SpacesBackend.cancelBooking(booking.id)
                    else -> error("Unsupported booking type: ${booking::class.simpleName}")
                }
                Sync.syncBookings()
                uiThread { onCancel() }
            } finally {
                _updatingBooking.emit(false)
            }
        }
    }

    fun confirmBooking(booking: BookingEntity<*>, onConfirm: () -> Unit) {
        launch {
            try {
                _updatingBooking.emit(true)
                when (booking) {
                    is ItemBooking -> InventoryBackend.confirm(booking.id)
                    is SpaceBooking -> SpacesBackend.confirm(booking.id)
                    else -> error("Unsupported booking type: ${booking::class.simpleName}")
                }
                Sync.syncBookings()
                uiThread { onConfirm() }
            } finally {
                _updatingBooking.emit(false)
            }
        }
    }

    fun markAsTaken(booking: BookingEntity<*>, meta: Map<String, Any>, onMarked: () -> Unit) {
        launch {
            try {
                _updatingBooking.emit(true)
                when (booking) {
                    is ItemBooking -> InventoryBackend.markTaken(booking.id)
                    is SpaceBooking -> {
                        val space = spacesDao.getSpaceById(booking.spaceId) ?: error("Space not found")
                        val keys = space.keys?.takeUnless { it.isEmpty() }
                        if (keys != null) {
                            val keyId = meta[BOOKING_CONFIRM_META_SPACE_KEY] as Int
                            SpacesBackend.markTaken(booking.id, keyId)
                        } else {
                            SpacesBackend.markTaken(booking.id)
                        }
                    }

                    else -> error("Unsupported booking type: ${booking::class.simpleName}")
                }
                Sync.syncBookings()
                uiThread { onMarked() }
            } finally {
                _updatingBooking.emit(false)
            }
        }
    }

    fun markAsReturned(booking: BookingEntity<*>, onMarked: () -> Unit) {
        launch {
            try {
                _updatingBooking.emit(true)
                when (booking) {
                    is ItemBooking -> InventoryBackend.markReturned(booking.id)
                    is SpaceBooking -> SpacesBackend.markReturned(booking.id)
                    else -> error("Unsupported booking type: ${booking::class.simpleName}")
                }
                Sync.syncBookings()
                uiThread { onMarked() }
            } finally {
                _updatingBooking.emit(false)
            }
        }
    }

    fun confirm(user: User, onConfirm: () -> Unit) {
        launch {
            try {
                _updatingUser.emit(true)
                UserDataBackend.confirm(user)
                Sync.syncBookings()
                uiThread { onConfirm() }
            } finally {
                _updatingUser.emit(false)
            }
        }
    }

    fun delete(user: User, onDelete: () -> Unit) {
        launch {
            try {
                _updatingUser.emit(true)
                UserDataBackend.delete(user)
                Sync.syncBookings()
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
