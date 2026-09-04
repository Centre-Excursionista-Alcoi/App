package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diamondedge.logging.logging
import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.protocol.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.centrexcursionistalcoi.app.di.DispatcherProvider
import org.centrexcursionistalcoi.app.network.Server
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.push.FCMTokenManager
import org.centrexcursionistalcoi.app.sync.BackgroundJobCoordinator
import org.centrexcursionistalcoi.app.sync.SyncAllDataBackgroundJob
import org.centrexcursionistalcoi.app.sync.await
import org.centrexcursionistalcoi.app.sync.copyToProgress
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class LoadingViewModel(
    private val dispatcherProvider: DispatcherProvider,
    private val backgroundJobCoordinator: BackgroundJobCoordinator,
) : ViewModel() {

    private val log = logging()

    val progress: StateFlow<Progress?>
        field = MutableStateFlow<Progress?>(null)

    val error: StateFlow<Throwable?>
        field = MutableStateFlow<Throwable?>(null)

    private val progressNotifier: ProgressNotifier = ProgressNotifier { progress.value = it }

    fun load(
        onLoggedIn: () -> Unit,
        onNotLoggedIn: () -> Unit,
    ) = viewModelScope.launch(dispatcherProvider.io) {
        log.d { "Loading app content..." }
        error.value = null

        Server.loadInfo()

        try {
            // Try to fetch the profile to see if the session is still valid
            if (isUserProfileValid()) {
                if (SyncAllDataBackgroundJob.databaseVersionUpgrade()) {
                    log.d { "Database migration, running synchronization..." }
                    backgroundJobCoordinator.schedule<SyncAllDataBackgroundJob>(
                        input = mapOf(SyncAllDataBackgroundJob.EXTRA_FORCE_SYNC to "true"),
                        requiresInternet = true,
                        uniqueName = SyncAllDataBackgroundJob.UNIQUE_NAME,
                    ).copyToProgress(progressNotifier, dispatcherProvider.io).await()
                } else {
                    log.d { "Scheduling periodic sync..." }
                    backgroundJobCoordinator.scheduleAsync<SyncAllDataBackgroundJob>(
                        input = mapOf(SyncAllDataBackgroundJob.EXTRA_FORCE_SYNC to "false"),
                        requiresInternet = true,
                        uniqueName = SyncAllDataBackgroundJob.UNIQUE_NAME,
                        repeatInterval = SyncAllDataBackgroundJob.periodicSyncInterval,
                    )
                }

                progress.value = null
                withContext(dispatcherProvider.main) { onLoggedIn() }
            } else {
                // Clear Sentry user context
                Sentry.configureScope { scope ->
                    scope.user = null
                }

                progress.value = null
                withContext(dispatcherProvider.main) { onNotLoggedIn() }
            }
        } catch (e: Exception) {
            log.e(e) { "Error while loading." }
            progress.value = null
            error.value = e
        }
    }

    /**
     * Returns true if the user should stay logged in, false otherwise.
     *
     * This function fetches the locally stored profile data. If the profile is found,
     * it updates the Sentry user context and renovates the FCM token if required. If the
     * profile is not found, it indicates that the user should be logged out.
     */
    private suspend fun isUserProfileValid(): Boolean {
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

        log.d { "Renovating FCM token if required" }
        FCMTokenManager.renovate()

        log.d { "Load finished!" }
        return true
    }
}
