package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.LendingsRepository
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.centrexcursionistalcoi.app.network.DepartmentsRemoteRepository
import org.centrexcursionistalcoi.app.nav.mainTabs
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository
import org.centrexcursionistalcoi.app.sync.BackgroundJobCoordinator
import org.centrexcursionistalcoi.app.sync.BackgroundJobState
import org.centrexcursionistalcoi.app.sync.SyncAllDataBackgroundJob
import org.centrexcursionistalcoi.app.ui.utils.departmentsCountBadge
import org.centrexcursionistalcoi.app.ui.utils.lendingsCountBadge
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class MainScreenViewModel(
    profileRepository: ProfileRepository,
    departmentsRepository: DepartmentsRepository,
    lendingsRepository: LendingsRepository,
    private val departmentsRemoteRepository: DepartmentsRemoteRepository,
    private val lendingsRemoteRepository: LendingsRemoteRepository,
    private val backgroundJobCoordinator: BackgroundJobCoordinator
) : ViewModel() {
    val isSyncing = backgroundJobCoordinator.observeUnique(SyncAllDataBackgroundJob.UNIQUE_NAME)
        .stateFlow()
        .map { it in listOf(BackgroundJobState.RUNNING) }
        .stateInViewModel()

    val profile = profileRepository.profile.stateInViewModel()
    val departments = departmentsRepository.selectAllAsFlow().stateInViewModel()
    val lendings = lendingsRepository.selectAllAsFlow().stateInViewModel()
    val activeUserLending = combine(profile, lendings) { profile, lendings ->
        val profileValue = profile ?: return@combine null
        lendings
            ?.filter { it.user.sub == profileValue.sub || it.user.sub == "unknown" }
            ?.find { it.status().isPending() }
    }.stateInViewModel()

    /** The tabs the user gets, `null` until their profile is loaded. See [mainTabs]. */
    val tabs = combine(profile, departments, activeUserLending) { profile, departments, activeLending ->
        profile?.let { mainTabs(it, departments, hasActiveLending = activeLending != null) }
    }.stateInViewModel()

    /** What the management tab's badge counts: requests to join departments and lendings waiting for a manager. */
    val managementBadge = combine(departments, lendings) { departments, lendings ->
        (departments.departmentsCountBadge() ?: 0) + (lendings.lendingsCountBadge() ?: 0)
    }.stateInViewModel(0)

    fun sync() = launch {
        backgroundJobCoordinator.schedule<SyncAllDataBackgroundJob>(
            name = SyncAllDataBackgroundJob.UNIQUE_NAME,
            input = mapOf(SyncAllDataBackgroundJob.EXTRA_FORCE_SYNC to "true"),
            requiresInternet = true,
            uniqueName = SyncAllDataBackgroundJob.UNIQUE_NAME
        )
    }

    fun cancelLending(lending: ReferencedLending) = launch {
        lendingsRemoteRepository.cancel(lending.id)
    }

    fun requestJoinDepartment(department: Department) = launch {
        departmentsRemoteRepository.requestJoin(department.id)
    }

    fun leaveDepartment(department: Department) = launch {
        departmentsRemoteRepository.leave(department.id)
    }
}
