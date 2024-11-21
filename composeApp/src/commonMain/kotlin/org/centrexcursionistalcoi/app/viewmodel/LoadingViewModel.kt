package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.mmk.kmpnotifier.notification.NotifierManager
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.centrexcursionistalcoi.app.auth.AccountManager
import org.centrexcursionistalcoi.app.network.AuthBackend
import org.centrexcursionistalcoi.app.network.Backend
import org.centrexcursionistalcoi.app.network.Sync
import org.centrexcursionistalcoi.app.push.PushTopic
import org.centrexcursionistalcoi.app.route.Home
import org.centrexcursionistalcoi.app.route.Login

class LoadingViewModel : ViewModel() {
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

                    Sync.syncBasics()

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
}
