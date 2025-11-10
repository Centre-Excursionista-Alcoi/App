package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.protocol.User
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.network.ProfileRemoteRepository
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.push.FCMTokenManager
import org.centrexcursionistalcoi.app.sync.BackgroundJobCoordinator
import org.centrexcursionistalcoi.app.sync.SyncAllDataBackgroundJob
import org.centrexcursionistalcoi.app.sync.SyncAllDataBackgroundJobLogic

@OptIn(ExperimentalTime::class)
class LoadingViewModel(
    onLoggedIn: () -> Unit,
    onNotLoggedIn: () -> Unit,
) : ViewModel() {
    companion object {
        suspend fun syncAll(force: Boolean = false, progressNotifier: ProgressNotifier? = null): Boolean {
            Napier.d { "Getting profile..." }
            val profile = ProfileRemoteRepository.getProfile(progressNotifier)
            return if (profile != null) {
                Napier.d { "User is logged in, updating cached profile data..." }
                ProfileRepository.update(profile)

                Napier.d { "Updating Sentry user context..." }
                Sentry.setUser(
                    User().apply {
                        id = profile.sub
                        email = profile.email
                    }
                )

                Napier.d { "Scheduling data sync..." }
                BackgroundJobCoordinator.schedule<SyncAllDataBackgroundJobLogic, SyncAllDataBackgroundJob>(
                    input = mapOf(SyncAllDataBackgroundJobLogic.EXTRA_FORCE_SYNC to "$force"),
                    requiresInternet = true,
                    uniqueName = SyncAllDataBackgroundJobLogic.UNIQUE_NAME,
                    logic = SyncAllDataBackgroundJobLogic,
                )

                Napier.d { "Renovating FCM token if required" }
                FCMTokenManager.renovate()

                Napier.d { "Load finished!" }
                true
            } else {
                Napier.i { "User is not logged in" }
                false
            }
        }
    }

    private val _progress = MutableStateFlow<Progress?>(null)
    val progress = _progress.asStateFlow()

    private val _error = MutableStateFlow<Throwable?>(null)
    val error = _error.asStateFlow()

    private val progressNotifier: ProgressNotifier = { progress ->
        _progress.value = progress
    }

    private fun load(
        onLoggedIn: () -> Unit,
        onNotLoggedIn: () -> Unit,
    ) = viewModelScope.launch(defaultAsyncDispatcher) {
        Napier.d { "Checking if user is logged in..." }
        _error.value = null

        try {
            // Try to fetch the profile to see if the session is still valid
            if (syncAll(progressNotifier = progressNotifier)) {
                Napier.d { "Scheduling periodic sync..." }
                BackgroundJobCoordinator.scheduleAsync<SyncAllDataBackgroundJobLogic, SyncAllDataBackgroundJob>(
                    input = mapOf(SyncAllDataBackgroundJobLogic.EXTRA_FORCE_SYNC to "false"),
                    requiresInternet = true,
                    uniqueName = SyncAllDataBackgroundJobLogic.UNIQUE_NAME,
                    repeatInterval = SyncAllDataBackgroundJobLogic.periodicSyncInterval,
                    logic = SyncAllDataBackgroundJobLogic,
                )

                _progress.value = null
                withContext(Dispatchers.Main) { onLoggedIn() }
            } else {
                // Clear Sentry user context
                Sentry.configureScope { scope ->
                    scope.user = null
                }

                _progress.value = null
                withContext(Dispatchers.Main) { onNotLoggedIn() }
            }
        } catch (e: Exception) {
            Napier.e("Error while loading.", e)
            _progress.value = null
            _error.value = e
        }
    }

    init {
        load(onLoggedIn, onNotLoggedIn)
    }
}
