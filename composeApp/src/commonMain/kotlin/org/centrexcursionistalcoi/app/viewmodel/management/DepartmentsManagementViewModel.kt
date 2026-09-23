package org.centrexcursionistalcoi.app.viewmodel.management

import androidx.lifecycle.ViewModel
import com.diamondedge.logging.logging
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.withContext
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.DepartmentMemberInfo
import org.centrexcursionistalcoi.app.data.DepartmentRole
import org.centrexcursionistalcoi.app.data.Qualification
import org.centrexcursionistalcoi.app.data.fileWithContext
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.centrexcursionistalcoi.app.database.UsersRepository
import org.centrexcursionistalcoi.app.di.DispatcherProvider
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.network.DepartmentsRemoteRepository
import org.centrexcursionistalcoi.app.network.QualificationsRemoteRepository
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
    private val departmentsRemoteRepository: DepartmentsRemoteRepository,
    private val qualificationsRemoteRepository: QualificationsRemoteRepository,
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

    fun updateMemberRoles(member: DepartmentMemberInfo, roles: List<DepartmentRole>) = launch {
        withContext(dispatcherProvider.io) {
            departmentsRemoteRepository.updateMemberRoles(member.departmentId, member.id, roles)
        }
    }

    fun createQualification(departmentId: Uuid, name: String, description: String?) = launch {
        withContext(dispatcherProvider.io) { qualificationsRemoteRepository.create(departmentId, name, description) }
    }

    fun updateQualification(qualification: Qualification, name: String, description: String?) = launch {
        val newName = name.takeIf { it != qualification.name }
        // A blank description clears it on the server, so "" is a real value here; only skip it if unchanged
        val newDescription = (description ?: "").takeIf { it != (qualification.description ?: "") }
        // Nothing changed: the server would answer NothingToUpdate
        if (newName == null && newDescription == null) return@launch

        withContext(dispatcherProvider.io) { qualificationsRemoteRepository.update(qualification.id, newName, newDescription) }
    }

    fun deleteQualification(qualification: Qualification) = launch {
        withContext(dispatcherProvider.io) { qualificationsRemoteRepository.delete(qualification.id) }
    }

    /**
     * Replaces [member]'s full set of held qualifications (among the department's own [currentlyHeldIds]) with
     * [newlyHeldIds], granting/revoking whichever changed -- the same single-button-click shape as
     * [updateMemberRoles], even though the server has no bulk endpoint for this (unlike role assignment) and
     * this issues one grant/revoke call per changed qualification.
     */
    fun updateMemberQualifications(member: DepartmentMemberInfo, currentlyHeldIds: Set<Uuid>, newlyHeldIds: Set<Uuid>) = launch {
        withContext(dispatcherProvider.io) {
            for (id in newlyHeldIds - currentlyHeldIds) qualificationsRemoteRepository.grant(id, member.userSub)
            for (id in currentlyHeldIds - newlyHeldIds) qualificationsRemoteRepository.revoke(id, member.userSub)
        }
    }
}
