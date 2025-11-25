package org.centrexcursionistalcoi.app.network

import io.github.aakira.napier.Napier
import io.ktor.client.request.post
import io.ktor.http.isSuccess
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.GlobalAsyncErrorHandler
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.DepartmentMemberInfo
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.error.bodyAsError
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.storage.InMemoryFileAllocator
import org.centrexcursionistalcoi.app.utils.Zero

object DepartmentsRemoteRepository : SymmetricRemoteRepository<Uuid, Department>(
    "/departments",
    Department.serializer(),
    DepartmentsRepository
) {
    suspend fun create(displayName: String, image: ByteArray?, progressNotifier: ProgressNotifier? = null) {
        val imageUuid = image?.let { InMemoryFileAllocator.put(it) }

        Napier.i { "Creating a new department: displayName=\"${displayName}\", imageUuid=${imageUuid}" }

        create(Department(Uuid.Zero, displayName, imageUuid?.id, emptyList()), progressNotifier)
    }

    suspend fun confirmJoinRequest(request: DepartmentMemberInfo) {
        Napier.i { "Confirming join request: departmentId=${request.departmentId}, requestId=${request.id}" }

        val response = httpClient.post("/departments/${request.departmentId}/confirm/${request.id}")
        if (response.status.isSuccess()) {
            Napier.i { "Join request confirmed successfully." }
            update(request.departmentId) // Refresh department data
        } else {
            // Try to decode the error
            val error = response.bodyAsError()
            Napier.e { "Failed to confirm join request: $error" }
            throw error.toThrowable().also(GlobalAsyncErrorHandler::setError)
        }
    }

    suspend fun denyJoinRequest(request: DepartmentMemberInfo) {
        Napier.i { "Denying join request: departmentId=${request.departmentId}, requestId=${request.id}" }

        val response = httpClient.post("/departments/${request.departmentId}/deny/${request.id}")
        if (response.status.isSuccess()) {
            Napier.i { "Join request denied successfully." }
            update(request.departmentId) // Refresh department data
        } else {
            // Try to decode the error
            val error = response.bodyAsError()
            Napier.e { "Failed to deny join request: $error" }
            throw error.toThrowable().also(GlobalAsyncErrorHandler::setError)
        }
    }

    suspend fun requestJoin(departmentId: Uuid) {
        Napier.i { "Requesting to join department: departmentId=$departmentId" }

        val response = httpClient.post("/departments/$departmentId/join")
        if (response.status.isSuccess()) {
            Napier.i { "Join request sent successfully." }
            update(departmentId) // Refresh department data
        } else {
            // Try to decode the error
            val error = response.bodyAsError()
            Napier.e { "Failed to send join request: $error" }
            throw error.toThrowable().also(GlobalAsyncErrorHandler::setError)
        }
    }
}
