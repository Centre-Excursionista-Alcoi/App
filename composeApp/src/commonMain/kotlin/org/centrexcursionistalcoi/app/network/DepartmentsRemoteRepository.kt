package org.centrexcursionistalcoi.app.network

import com.diamondedge.logging.logging
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import org.centrexcursionistalcoi.app.GlobalAsyncErrorHandler
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.DepartmentMemberInfo
import org.centrexcursionistalcoi.app.data.DepartmentRole
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import org.centrexcursionistalcoi.app.error.bodyAsError
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.request.UpdateDepartmentMemberRolesRequest
import org.centrexcursionistalcoi.app.storage.InMemoryFileAllocator
import org.centrexcursionistalcoi.app.storage.SETTINGS_LAST_DEPARTMENTS_SYNC
import org.centrexcursionistalcoi.app.utils.Zero
import org.koin.core.annotation.Singleton
import kotlin.uuid.Uuid

@Singleton
class DepartmentsRemoteRepository(
    departmentsRepository: DepartmentsRepository,
    private val inventoryItemTypesRepository: InventoryItemTypesRepository
) : SymmetricRemoteRepository<Uuid, Department>(
    "/departments",
    SETTINGS_LAST_DEPARTMENTS_SYNC,
    Department.serializer(),
    departmentsRepository
) {
    private val log = logging()

    suspend fun create(displayName: String, image: ByteArray?, progressNotifier: ProgressNotifier? = null) {
        val imageUuid = image?.let { InMemoryFileAllocator.put(it) }

        log.i { "Creating a new department: displayName=\"${displayName}\", imageUuid=${imageUuid}" }

        create(Department(Uuid.Zero, displayName, imageUuid?.id, emptyList()), progressNotifier)
    }

    suspend fun confirmJoinRequest(request: DepartmentMemberInfo) {
        log.i { "Confirming join request: departmentId=${request.departmentId}, requestId=${request.id}" }

        val response = httpClient.post("/departments/${request.departmentId}/confirm/${request.id}")
        if (response.status.isSuccess()) {
            log.i { "Join request confirmed successfully." }
            update(request.departmentId, ignoreIfModifiedSince = true) // Refresh department data
        } else {
            // Try to decode the error
            val error = response.bodyAsError()
            log.e { "Failed to confirm join request: $error" }
            throw error.toThrowable().also(GlobalAsyncErrorHandler::setError)
        }
    }

    suspend fun denyJoinRequest(request: DepartmentMemberInfo) {
        log.i { "Denying join request: departmentId=${request.departmentId}, requestId=${request.id}" }

        val response = httpClient.post("/departments/${request.departmentId}/deny/${request.id}")
        if (response.status.isSuccess()) {
            log.i { "Join request denied successfully." }
            update(request.departmentId, ignoreIfModifiedSince = true) // Refresh department data
        } else {
            // Try to decode the error
            val error = response.bodyAsError()
            log.e { "Failed to deny join request: $error" }
            throw error.toThrowable().also(GlobalAsyncErrorHandler::setError)
        }
    }

    suspend fun requestJoin(departmentId: Uuid) {
        log.i { "Requesting to join department: departmentId=$departmentId" }

        val response = httpClient.post("/departments/$departmentId/join")
        if (response.status.isSuccess()) {
            log.i { "Join request sent successfully." }
            update(departmentId, ignoreIfModifiedSince = true) // Refresh department data
        } else {
            // Try to decode the error
            val error = response.bodyAsError()
            log.e { "Failed to send join request: $error" }
            throw error.toThrowable().also(GlobalAsyncErrorHandler::setError)
        }
    }

    suspend fun leave(departmentId: Uuid) {
        log.i { "Leaving department $departmentId..." }

        val response = httpClient.post("/departments/$departmentId/leave")
        if (response.status.isSuccess()) {
            log.i { "Left department successfully" }

            // Clean up inventory items and item types associated with this department
            log.i { "Deleting items associated with the left department..." }
            inventoryItemTypesRepository.deleteByDepartmentId(departmentId)

            log.i { "Updating locally stored department..." }
            update(departmentId, ignoreIfModifiedSince = true) // Refresh department data
        } else {
            // Try to decode the error
            val error = response.bodyAsError()
            log.e { "Failed to leave department: $error" }
            throw error.toThrowable().also(GlobalAsyncErrorHandler::setError)
        }
    }

    suspend fun kick(departmentId: Uuid, sub: String) {
        log.i { "Kicking user $sub from department $departmentId..." }

        val response = httpClient.post("/departments/$departmentId/leave/$sub")
        if (response.status.isSuccess()) {
            log.i { "Kicked from department successfully" }
            update(departmentId, ignoreIfModifiedSince = true) // Refresh department data
        } else {
            // Try to decode the error
            val error = response.bodyAsError()
            log.e { "Failed to kick from department: $error" }
            throw error.toThrowable().also(GlobalAsyncErrorHandler::setError)
        }
    }

    /**
     * Replaces [memberId]'s full set of [DepartmentRole]s within [departmentId]. Requires the caller to be a
     * global admin or hold [DepartmentRole.ADMIN] in that department -- enforced server-side, since assigning
     * roles (including ADMIN itself) is privilege-escalation-capable.
     */
    suspend fun updateMemberRoles(departmentId: Uuid, memberId: Uuid, roles: List<DepartmentRole>) {
        log.i { "Updating roles for member $memberId in department $departmentId: $roles" }

        val response = httpClient.patch("/departments/$departmentId/members/$memberId/roles") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdateDepartmentMemberRolesRequest.serializer(), UpdateDepartmentMemberRolesRequest(roles)))
        }
        if (response.status.isSuccess()) {
            log.i { "Roles updated successfully." }
            update(departmentId, ignoreIfModifiedSince = true) // Refresh department data
        } else {
            // Try to decode the error
            val error = response.bodyAsError()
            log.e { "Failed to update member roles: $error" }
            throw error.toThrowable().also(GlobalAsyncErrorHandler::setError)
        }
    }
}
