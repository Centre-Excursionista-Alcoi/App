package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.storage.SettingsCookiesStorage
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class LoadingViewModel : ViewModelBase() {
    fun load(
        onLoggedIn: (name: String, groups: List<String>) -> Unit,
        onNotLoggedIn: () -> Unit,
    ) = viewModelScope.launch {
        val isLoggedIn = SettingsCookiesStorage.Default.contains("USER_SESSION")
        if (!isLoggedIn) {
            Napier.w { "Not logged in: USER_SESSION is not in cookies" }
            Napier.d { "Cookies: ${SettingsCookiesStorage.Default.getCookies()}" }
            return@launch onNotLoggedIn()
        }

        val response = client.get("/profile")
        val body = response.bodyAsText()
        json.decodeFromString(ProfileResponse.serializer(), body).let {
            onLoggedIn(it.username, it.groups)
        }
    }
}
