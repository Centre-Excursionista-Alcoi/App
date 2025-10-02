package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.viewModelScope
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.platform.PlatformLoadLogic
import org.centrexcursionistalcoi.app.response.ProfileResponse

@OptIn(ExperimentalTime::class)
class LoadingViewModel : ViewModelBase() {
    fun load(
        onLoggedIn: (name: String, groups: List<String>) -> Unit,
        onNotLoggedIn: () -> Unit,
    ) = viewModelScope.launch(defaultAsyncDispatcher) {
        PlatformLoadLogic.load()

        // Try to fetch the profile to see if the session is still valid
        val response = client.get("/profile")
        if (response.status.isSuccess()) {
            val body = response.bodyAsText()
            json.decodeFromString(ProfileResponse.serializer(), body).let {
                onLoggedIn(it.username, it.groups)
            }
        } else {
            onNotLoggedIn()
        }
    }
}
