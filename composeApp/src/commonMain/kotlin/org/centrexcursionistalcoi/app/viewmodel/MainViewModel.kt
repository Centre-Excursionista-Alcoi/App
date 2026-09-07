package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import com.diamondedge.logging.logging
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.database.LendingsRepository
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository
import org.centrexcursionistalcoi.app.sync.BackgroundJobCoordinator
import org.centrexcursionistalcoi.app.sync.BackgroundJobState
import org.centrexcursionistalcoi.app.sync.SyncAllDataBackgroundJob
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class MainViewModel(
    private val lendingsRepository: LendingsRepository,
    private val lendingsRemoteRepository: LendingsRemoteRepository,
    private val backgroundJobCoordinator: BackgroundJobCoordinator
) : ViewModel() {
    companion object {
        private val log = logging()
    }

    val isSyncing = backgroundJobCoordinator.observeUnique(SyncAllDataBackgroundJob.UNIQUE_NAME)
        .stateFlow()
        .map { it in listOf(BackgroundJobState.RUNNING) }
        .stateInViewModel()

    val profile = ProfileRepository.profile.stateInViewModel()

    val lendings = lendingsRepository.selectAllAsFlow().stateInViewModel()

    fun cancelLending(lending: ReferencedLending) = launch {
        lendingsRemoteRepository.cancel(lending.id)
    }

    fun sync() = launch {
        log.d { "Scheduling data sync..." }
        backgroundJobCoordinator.schedule<SyncAllDataBackgroundJob>(
            name = SyncAllDataBackgroundJob.UNIQUE_NAME,
            input = mapOf(SyncAllDataBackgroundJob.EXTRA_FORCE_SYNC to "true"),
            requiresInternet = true,
            uniqueName = SyncAllDataBackgroundJob.UNIQUE_NAME,
        )
    }

    fun deleteLending(lending: ReferencedLending, reason: String?) = launch {
        lendingsRemoteRepository.delete(lending.id, reason)
    }
}
