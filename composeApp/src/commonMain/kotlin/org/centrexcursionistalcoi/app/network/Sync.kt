package org.centrexcursionistalcoi.app.network

import com.russhwolf.settings.ExperimentalSettingsApi
import org.centrexcursionistalcoi.app.data.UserD
import org.centrexcursionistalcoi.app.database.appDatabase
import org.centrexcursionistalcoi.app.database.entity.admin.User
import org.centrexcursionistalcoi.app.database.updateEntities
import org.centrexcursionistalcoi.app.serverJson
import org.centrexcursionistalcoi.app.settings.SettingsKeys
import org.centrexcursionistalcoi.app.settings.settings

object Sync {
    private val bookingsDao = appDatabase.bookingsDao()
    private val inventoryDao = appDatabase.inventoryDao()
    private val spacesDao = appDatabase.spacesDao()
    private val adminDao = appDatabase.adminDao()

    /**
     * Sync the basic data from the server to the local database.
     * - User data
     * - Sections
     * - Item types
     * - Items
     * - Spaces
     */
    @OptIn(ExperimentalSettingsApi::class)
    suspend fun syncBasics() {
        val userData = UserDataBackend.getUserData()
        settings.putString(SettingsKeys.USER_DATA, serverJson.encodeToString(UserD.serializer(), userData))

        val sections = SectionsBackend.list()
        updateEntities(
            list = sections,
            inventoryDao::getAllSections,
            inventoryDao::insertSection,
            inventoryDao::updateSection,
            inventoryDao::deleteSection
        )

        val types = InventoryBackend.listTypes()
        updateEntities(
            list = types,
            inventoryDao::getAllItemTypes,
            inventoryDao::insertItemType,
            inventoryDao::updateItemType,
            inventoryDao::deleteItemType
        )

        val items = InventoryBackend.listItems()
        updateEntities(
            list = items,
            inventoryDao::getAllItems,
            inventoryDao::insertItem,
            inventoryDao::updateItem,
            inventoryDao::deleteItem
        )

        val spaces = SpacesBackend.list()
        updateEntities(
            list = spaces,
            spacesDao::getAllSpaces,
            spacesDao::insertSpace,
            spacesDao::updateSpace,
            spacesDao::deleteSpace
        )
    }

    @OptIn(ExperimentalSettingsApi::class)
    suspend fun syncBookings() {
        val itemBookings = InventoryBackend.listBookings().toMutableList()
        val spaceBookings = SpacesBackend.listBookings().toMutableList()

        val userData = settings.getStringOrNull(SettingsKeys.USER_DATA)
            ?.let { serverJson.decodeFromString(UserD.serializer(), it) }

        // only for administrators
        if (userData?.isAdmin == true) {
            val usersList = UserDataBackend.listUsers()
            updateEntities(
                list = usersList,
                idExtractor = User::email,
                adminDao::getAllUsers,
                adminDao::insertUser,
                adminDao::updateUser,
                adminDao::deleteUser
            )

            val allItemBookings = InventoryBackend.allBookings()
            itemBookings += allItemBookings.filter { booking -> itemBookings.none { it.id == booking.id } }

            val allSpaceBookings = SpacesBackend.allBookings()
            spaceBookings += allSpaceBookings.filter { booking -> spaceBookings.none { it.id == booking.id } }
        }

        updateEntities(
            list = itemBookings,
            bookingsDao::getAllItemBookings,
            bookingsDao::insertItemBooking,
            bookingsDao::updateItemBooking,
            bookingsDao::deleteItemBooking
        )
        updateEntities(
            list = spaceBookings,
            bookingsDao::getAllSpaceBookings,
            bookingsDao::insertSpaceBooking,
            bookingsDao::updateSpaceBooking,
            bookingsDao::deleteSpaceBooking
        )
    }
}
