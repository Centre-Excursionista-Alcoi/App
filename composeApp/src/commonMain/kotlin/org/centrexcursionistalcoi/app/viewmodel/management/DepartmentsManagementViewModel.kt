package org.centrexcursionistalcoi.app.viewmodel.management

import androidx.lifecycle.ViewModel
import com.diamondedge.logging.logging
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.withContext
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.DepartmentMemberInfo
import org.centrexcursionistalcoi.app.data.fileWithContext
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.centrexcursionistalcoi.app.database.UsersRepository
import org.centrexcursionistalcoi.app.di.DispatcherProvider
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.network.DepartmentsRemoteRepository
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.request.UpdateDepartmentRequest
import org.centrexcursionistalcoi.app.viewmodel.launch
import org.centrexcursionistalcoi.app.viewmodel.stateInViewModel
import org.koin.core.annotation.KoinViewModel
import kotlin.uuid.Uuid

@KoinViewModel
class DepartmentsManagementViewModel(
    private val dispatcherProvider: DispatcherProvider,
    departmentsRepository: DepartmentsRepository,
    usersRepository: UsersRepository,
    private val departmentsRemoteRepository: DepartmentsRemoteRepository
) : ViewModel() {
    companion object {
        private val log = logging()
    }

    val profile = ProfileRepository.profile.stateInViewModel()
    val departments = departmentsRepository.selectAllAsFlow().stateInViewModel()
    val users = usersRepository.selectAllAsFlow().stateInViewModel()

    fun createDepartment(
        displayName: String,
        imageFile: PlatformFile?,
        progressNotifier: ProgressNotifier?
    ) = launch {
        try {
            withContext(dispatcherProvider.io) {
                val image = imageFile?.readBytes()
                departmentsRemoteRepository.create(displayName, image, progressNotifier)
            }
        } catch (e: ServerException) {
            log.e(e) { "Could not create department." }
        } catch (e: Exception) {
            log.e(e) { "Could not create department due to an unexpected error." }
        }
    }

    fun updateDepartment(
        departmentId: Uuid,
        displayName: String,
        image: PlatformFile?,
        progressNotifier: ProgressNotifier? = null,
    ) = launch {
        withContext(dispatcherProvider.io) {
            departmentsRemoteRepository.update(
                departmentId,
                UpdateDepartmentRequest(
                    displayName = displayName,
                    image = image?.fileWithContext(),
                ),
                UpdateDepartmentRequest.serializer(),
                progressNotifier,
            )
        }
    }

    fun delete(department: Department) = launch {
        withContext(dispatcherProvider.io) {
            departmentsRemoteRepository.delete(department.id)
        }
    }

    fun approveDepartmentJoinRequest(request: DepartmentMemberInfo) = launch {
        departmentsRemoteRepository.confirmJoinRequest(request)
    }

    fun denyDepartmentJoinRequest(request: DepartmentMemberInfo) = launch {
        departmentsRemoteRepository.denyJoinRequest(request)
    }
}
