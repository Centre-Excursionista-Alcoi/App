package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diamondedge.logging.logging
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
        private val log = logging()

        suspend fun syncAll(force: Boolean = false): Boolean {
            log.d { "Fetching locally stored profile data." }
            ProfileRepository.getProfile()?.let { profile ->
                log.d { "Updating Sentry user context..." }
                Sentry.setUser(
                    User().apply {
                        id = profile.sub
                        email = profile.email
                    }
                )
            } ?: run {
                log.d { "Profile not locally stored, logging out..." }
                return false
            }

            log.d { "Scheduling data sync..." }
            BackgroundJobCoordinator.schedule<SyncAllDataBackgroundJobLogic, SyncAllDataBackgroundJob>(
                input = mapOf(SyncAllDataBackgroundJobLogic.EXTRA_FORCE_SYNC to "$force"),
                requiresInternet = true,
                uniqueName = SyncAllDataBackgroundJobLogic.UNIQUE_NAME,
                logic = SyncAllDataBackgroundJobLogic,
            )

            log.d { "Renovating FCM token if required" }
            FCMTokenManager.renovate()

            log.d { "Load finished!" }
            return true
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
        log.d { "Loading app content..." }
        _error.value = null

        try {
            // Try to fetch the profile to see if the session is still valid
            if (syncAll()) {
                if (SyncAllDataBackgroundJobLogic.databaseVersionUpgrade()) {
                    log.d { "Database migration, running synchronization..." }
                    SyncAllDataBackgroundJobLogic.syncAll(true, progressNotifier)
                } else {
                    log.d { "Scheduling periodic sync..." }
                    BackgroundJobCoordinator.scheduleAsync<SyncAllDataBackgroundJobLogic, SyncAllDataBackgroundJob>(
                        input = mapOf(SyncAllDataBackgroundJobLogic.EXTRA_FORCE_SYNC to "false"),
                        requiresInternet = true,
                        uniqueName = SyncAllDataBackgroundJobLogic.UNIQUE_NAME,
                        repeatInterval = SyncAllDataBackgroundJobLogic.periodicSyncInterval,
                        logic = SyncAllDataBackgroundJobLogic,
                    )
                }

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
            log.e(e) { "Error while loading." }
            _progress.value = null
            _error.value = e
        }
    }

    init {
        load(onLoggedIn, onNotLoggedIn)
    }
}
