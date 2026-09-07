package org.centrexcursionistalcoi.app.viewmodel.management

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.withContext
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.MembersRepository
import org.centrexcursionistalcoi.app.database.UsersRepository
import org.centrexcursionistalcoi.app.di.DispatcherProvider
import org.centrexcursionistalcoi.app.network.DepartmentsRemoteRepository
import org.centrexcursionistalcoi.app.network.UsersRemoteRepository
import org.centrexcursionistalcoi.app.viewmodel.launch
import org.centrexcursionistalcoi.app.viewmodel.stateInViewModel
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class UsersManagementViewModel(
    private val dispatcherProvider: DispatcherProvider,
    departmentsRepository: DepartmentsRepository,
    membersRepository: MembersRepository,
    usersRepository: UsersRepository,
    private val departmentsRemoteRepository: DepartmentsRemoteRepository,
    private val usersRemoteRepository: UsersRemoteRepository,
) : ViewModel() {
    val departments = departmentsRepository.selectAllAsFlow().stateInViewModel()
    val members = membersRepository.selectAllAsFlow().stateInViewModel()
    val users = usersRepository.selectAllAsFlow().stateInViewModel()

    fun kickFromDepartment(userData: UserData, department: Department) = launch {
        withContext(dispatcherProvider.io) {
            departmentsRemoteRepository.kick(department.id, userData.sub)
        }
    }

    fun promote(user: UserData) = launch {
        withContext(dispatcherProvider.io) {
            usersRemoteRepository.promote(user.sub)
            usersRemoteRepository.update(user.sub, ignoreIfModifiedSince = true)
        }
    }
}
