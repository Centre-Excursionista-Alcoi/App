package org.centrexcursionistalcoi.app.network

import com.diamondedge.logging.logging
import io.ktor.client.request.*
import io.ktor.http.*
import org.centrexcursionistalcoi.app.GlobalAsyncErrorHandler
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.DepartmentMemberInfo
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.error.bodyAsError
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.storage.InMemoryFileAllocator
import org.centrexcursionistalcoi.app.storage.SETTINGS_LAST_DEPARTMENTS_SYNC
import org.centrexcursionistalcoi.app.utils.Zero
import kotlin.uuid.Uuid

object DepartmentsRemoteRepository : SymmetricRemoteRepository<Uuid, Department>(
    "/departments",
    SETTINGS_LAST_DEPARTMENTS_SYNC,
    Department.serializer(),
    DepartmentsRepository
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
}
