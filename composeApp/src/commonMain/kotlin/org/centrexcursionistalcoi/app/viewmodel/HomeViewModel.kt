package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.centrexcursionistalcoi.app.auth.AccountManager
import org.centrexcursionistalcoi.app.network.InventoryBackend
import org.centrexcursionistalcoi.app.network.UserDataBackend
import org.centrexcursionistalcoi.app.server.response.data.ItemTypeD
import org.centrexcursionistalcoi.app.server.response.data.UserD

class HomeViewModel : ViewModel() {
    private val _userData = MutableStateFlow<UserD?>(null)
    val userData get() = _userData.asStateFlow()

    private val _itemTypes = MutableStateFlow<List<ItemTypeD>?>(null)
    val itemTypes get() = _itemTypes.asStateFlow()

    fun load() {
        launch {
            val data = UserDataBackend.getUserData()
            _userData.emit(data)

            val types = InventoryBackend.listTypes()
            _itemTypes.emit(types)
        }
    }

    fun logout() {
        launch {
            AccountManager.logout()
        }
    }
}
