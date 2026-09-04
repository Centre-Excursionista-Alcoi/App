package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.data.isStub
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.LendingsRepository
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.centrexcursionistalcoi.app.network.DepartmentsRemoteRepository
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository
import org.centrexcursionistalcoi.app.sync.BackgroundJobCoordinator
import org.centrexcursionistalcoi.app.sync.BackgroundJobState
import org.centrexcursionistalcoi.app.sync.SyncAllDataBackgroundJob
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class MainScreenViewModel(
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

    val profile = ProfileRepository.profile.stateInViewModel()
    val departments = departmentsRepository.selectAllAsFlow().stateInViewModel()
    val lendings = lendingsRepository.selectAllAsFlow().stateInViewModel()
    val activeUserLending = combine(profile, lendings) { profile, lendings ->
        val profileValue = profile ?: return@combine null
        lendings
            ?.filter { it.user.sub == profileValue.sub || it.user.isStub() }
            ?.find { it.status().isPending() }
    }.stateInViewModel()

    fun sync() = launch {
        backgroundJobCoordinator.schedule<SyncAllDataBackgroundJob>(
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
