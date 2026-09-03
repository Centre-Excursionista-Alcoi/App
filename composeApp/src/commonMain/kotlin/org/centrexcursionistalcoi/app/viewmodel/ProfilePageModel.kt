package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Deferred
import kotlinx.datetime.LocalDate
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.network.DepartmentsRemoteRepository
import org.centrexcursionistalcoi.app.network.ProfileRemoteRepository
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class ProfilePageModel(
    departmentsRepository: DepartmentsRepository,
    private val departmentsRemoteRepository: DepartmentsRemoteRepository,
) : ViewModel() {
    val profile = ProfileRepository.profile.stateInViewModel()
    val departments = departmentsRepository.selectAllAsFlow().stateInViewModel()

    fun createInsurance(company: String, policyNumber: String, validFrom: LocalDate, validTo: LocalDate, document: PlatformFile?) = launch {
        ProfileRemoteRepository.createInsurance(company, policyNumber, validFrom, validTo, document)
        ProfileRemoteRepository.synchronize()
    }

    fun connectFEMECV(username: String, password: CharArray): Deferred<Throwable?> = async {
        try {
            ProfileRemoteRepository.connectFEMECV(username, password)
            null
        } catch (e: ServerException) {
            e
        }
    }

    fun disconnectFEMECV() = launch {
        ProfileRemoteRepository.disconnectFEMECV()
    }

    fun requestJoinDepartment(department: Department) = launch {
        departmentsRemoteRepository.requestJoin(department.id)
    }

    fun leaveDepartment(department: Department) = launch {
        departmentsRemoteRepository.leave(department.id)
    }
}
