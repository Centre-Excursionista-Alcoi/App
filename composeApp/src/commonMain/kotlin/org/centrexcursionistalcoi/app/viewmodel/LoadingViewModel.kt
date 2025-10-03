package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.viewModelScope
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.network.DepartmentsRemoteRepository
import org.centrexcursionistalcoi.app.network.PostsRemoteRepository
import org.centrexcursionistalcoi.app.network.ProfileRemoteRepository
import org.centrexcursionistalcoi.app.platform.PlatformLoadLogic

@OptIn(ExperimentalTime::class)
class LoadingViewModel : ViewModelBase() {
    fun load(
        onLoggedIn: () -> Unit,
        onNotLoggedIn: () -> Unit,
    ) = viewModelScope.launch(defaultAsyncDispatcher) {
        PlatformLoadLogic.load()

        // Try to fetch the profile to see if the session is still valid
        val profile = ProfileRemoteRepository.getProfile()
        if (profile != null) {
            DepartmentsRemoteRepository.synchronizeWithDatabase()
            PostsRemoteRepository.synchronizeWithDatabase()

            ProfileRepository.update(profile)
            onLoggedIn()
        } else {
            onNotLoggedIn()
        }
    }
}
