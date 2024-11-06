package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.centrexcursionistalcoi.app.auth.AccountManager
import org.centrexcursionistalcoi.app.network.UserDataBackend
import org.centrexcursionistalcoi.app.server.response.data.UserD

class HomeViewModel : ViewModel() {
    private val _userData = MutableStateFlow<UserD?>(null)
    val userData get() = _userData.asStateFlow()

    fun load() {
        launch {
            val data = UserDataBackend.getUserData()
            _userData.emit(data)
        }
    }

    fun logout() {
        launch {
            AccountManager.logout()
        }
    }
}
