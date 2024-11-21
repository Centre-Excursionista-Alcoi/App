package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.mmk.kmpnotifier.notification.NotifierManager
import com.russhwolf.settings.ExperimentalSettingsApi
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.centrexcursionistalcoi.app.auth.AccountManager
import org.centrexcursionistalcoi.app.data.UserD
import org.centrexcursionistalcoi.app.database.getRoomDatabase
import org.centrexcursionistalcoi.app.database.updateEntities
import org.centrexcursionistalcoi.app.network.AuthBackend
import org.centrexcursionistalcoi.app.network.Backend
import org.centrexcursionistalcoi.app.network.InventoryBackend
import org.centrexcursionistalcoi.app.network.SectionsBackend
import org.centrexcursionistalcoi.app.network.SpacesBackend
import org.centrexcursionistalcoi.app.network.UserDataBackend
import org.centrexcursionistalcoi.app.push.PushTopic
import org.centrexcursionistalcoi.app.route.Home
import org.centrexcursionistalcoi.app.route.Login
import org.centrexcursionistalcoi.app.serverJson
import org.centrexcursionistalcoi.app.settings.SettingsKeys
import org.centrexcursionistalcoi.app.settings.settings

class LoadingViewModel : ViewModel() {
    private val appDatabase = getRoomDatabase()
    private val bookingsDao = appDatabase.bookingsDao()
    private val inventoryDao = appDatabase.inventoryDao()
    private val spacesDao = appDatabase.spacesDao()

    private val _serverError = MutableStateFlow<Pair<String?, Exception?>?>(null)
    val serverError = _serverError.asStateFlow()

    private val _serverAvailable = MutableStateFlow<Boolean?>(null)
    val serverAvailable = _serverAvailable.asStateFlow()

    fun load(navController: NavController) {
        launch {
            // ping the server
            val serverAvailable = Backend.ping { body, exception ->
                _serverError.emit(body to exception)
            }
            if (!serverAvailable) {
                _serverAvailable.emit(false)
                return@launch
            } else {
                _serverAvailable.emit(true)
            }

            try {
                val account = AccountManager.get()
                if (account != null) {
                    // Login again to refresh the token
                    AuthBackend.login(account.first.email, account.second)

                    // Subscribe to the user's topic
                    val topic = PushTopic.topic(account.first.email)
                    Napier.i { "Subscribing to topic for FCM notifications: $topic" }
                    NotifierManager.getPushNotifier().subscribeToTopic(topic)

                    updateLocalData()

                    uiThread { navController.navigate(Home) }
                } else {
                    uiThread { navController.navigate(Login) }
                }
            } catch (e: Exception) {
                Napier.e(e) { "Could not log in." }

                uiThread { navController.navigate(Login) }
            }
        }
    }

    @OptIn(ExperimentalSettingsApi::class)
    private suspend fun updateLocalData() {
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
}
