package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.network.DepartmentsRemoteRepository
import org.centrexcursionistalcoi.app.network.PostsRemoteRepository
import org.centrexcursionistalcoi.app.network.ProfileRemoteRepository
import org.centrexcursionistalcoi.app.network.UsersRemoteRepository

@OptIn(ExperimentalTime::class)
class LoadingViewModel(
    onLoggedIn: () -> Unit,
    onNotLoggedIn: () -> Unit,
) : ViewModel() {
    private fun load(
        onLoggedIn: () -> Unit,
        onNotLoggedIn: () -> Unit,
    ) = viewModelScope.launch(defaultAsyncDispatcher) {
        Napier.d { "Checking if user is logged in..." }

        // Try to fetch the profile to see if the session is still valid
        val profile = ProfileRemoteRepository.getProfile()
        if (profile != null) {
            Napier.d { "User is logged in, updating cached profile data..." }
            ProfileRepository.update(profile)

            // Session is valid, synchronize the local database with the remote data
            Napier.d { "Synchronizing local data with remote..." }
            DepartmentsRemoteRepository.synchronizeWithDatabase()
            PostsRemoteRepository.synchronizeWithDatabase()
            if (profile.isAdmin) {
                Napier.d { "Synchronizing admin local data with remote..." }
                UsersRemoteRepository.synchronizeWithDatabase()
            }

            Napier.d { "Load finished!" }
            withContext(Dispatchers.Main) { onLoggedIn() }
        } else {
            Napier.i { "User is not logged in" }
            withContext(Dispatchers.Main) { onNotLoggedIn() }
        }
    }

    init {
        load(onLoggedIn, onNotLoggedIn)
    }
}
