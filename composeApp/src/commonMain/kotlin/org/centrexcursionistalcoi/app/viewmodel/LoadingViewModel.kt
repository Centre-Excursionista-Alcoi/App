package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.until
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.network.DepartmentsRemoteRepository
import org.centrexcursionistalcoi.app.network.InventoryItemTypesRemoteRepository
import org.centrexcursionistalcoi.app.network.InventoryItemsRemoteRepository
import org.centrexcursionistalcoi.app.network.PostsRemoteRepository
import org.centrexcursionistalcoi.app.network.ProfileRemoteRepository
import org.centrexcursionistalcoi.app.network.UsersRemoteRepository
import org.centrexcursionistalcoi.app.storage.settings

@OptIn(ExperimentalTime::class)
class LoadingViewModel(
    onLoggedIn: () -> Unit,
    onNotLoggedIn: () -> Unit,
) : ViewModel() {
    companion object {
        suspend fun syncAll(force: Boolean = false): Boolean {
            val profile = ProfileRemoteRepository.getProfile()
            return if (profile != null) {
                Napier.d { "User is logged in, updating cached profile data..." }
                ProfileRepository.update(profile)

                val lastSync = settings.getLongOrNull("lastSync")?.let { Instant.fromEpochSeconds(it) }
                val now = Clock.System.now()
                if (force || lastSync == null || lastSync.until(now, DateTimeUnit.SECOND) > 60 * 60) {
                    Napier.d { "Last sync was more than an hour ago, synchronizing data..." }

                    // Synchronize the local database with the remote data
                    DepartmentsRemoteRepository.synchronizeWithDatabase()
                    PostsRemoteRepository.synchronizeWithDatabase()
                    InventoryItemTypesRemoteRepository.synchronizeWithDatabase()
                    InventoryItemsRemoteRepository.synchronizeWithDatabase()

                    if (profile.isAdmin) {
                        Napier.d { "Synchronizing admin local data with remote..." }
                        UsersRemoteRepository.synchronizeWithDatabase()
                    }

                    settings.putLong("lastSync", now.epochSeconds)
                } else {
                    Napier.d { "Last sync was less than an hour ago, skipping synchronization." }
                }

                Napier.d { "Load finished!" }
                true
            } else {
                Napier.i { "User is not logged in" }
                false
            }
        }
    }

    private fun load(
        onLoggedIn: () -> Unit,
        onNotLoggedIn: () -> Unit,
    ) = viewModelScope.launch(defaultAsyncDispatcher) {
        Napier.d { "Checking if user is logged in..." }

        // Try to fetch the profile to see if the session is still valid
        if (syncAll()) {
            withContext(Dispatchers.Main) { onLoggedIn() }
        } else {
            withContext(Dispatchers.Main) { onNotLoggedIn() }
        }
    }

    init {
        load(onLoggedIn, onNotLoggedIn)
    }
}
