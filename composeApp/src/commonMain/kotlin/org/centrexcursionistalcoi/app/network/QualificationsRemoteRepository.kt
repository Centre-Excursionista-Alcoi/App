package org.centrexcursionistalcoi.app.network

import com.diamondedge.logging.logging
import io.ktor.client.request.delete
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.KSerializer
import org.centrexcursionistalcoi.app.GlobalAsyncErrorHandler
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.Qualification
import org.centrexcursionistalcoi.app.data.QualificationGrant
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.error.bodyAsError
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.request.CreateQualificationRequest
import org.centrexcursionistalcoi.app.request.GrantQualificationRequest
import org.centrexcursionistalcoi.app.request.UpdateQualificationRequest
import org.koin.core.annotation.Singleton
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Talks to the server's qualification mutation routes: create/update/delete a definition, grant/revoke it to a
 * member (see `DepartmentsManagementViewModel`, which surfaces all of this inside the Departments management
 * screen, right alongside member role assignment).
 *
 * Reading qualifications and their grants doesn't go through here: they're embedded on
 * [org.centrexcursionistalcoi.app.data.Department] and synced along with it (see `Departments.extraColumns`
 * server-side), like any other referenced data. Each mutation below patches the affected department's embedded
 * copy in [departmentsRepository] locally from the response, the same way [SymmetricRemoteRepository]-backed
 * repositories keep their own local table in sync with a create/update/delete -- so callers never need to force
 * a re-fetch of the department afterwards. Every mutation here requires at least `EXAMINER`
 * (`create`/`update`/`delete` require `QUALIFICATIONS_MANAGER`, which implies it), so the caller always already
 * holds the department's full, unfiltered local qualifications/grants -- patching in place never narrows what
 * they'd otherwise see from a fresh fetch.
 */
@Singleton
class QualificationsRemoteRepository(private val departmentsRepository: DepartmentsRepository) {
    private val log = logging()

    // Not cached in a field: tests swap the shared client (see `_httpClient`)
    private val httpClient get() = getHttpClient()

    /**
     * Throws the server's error, after reporting it to [GlobalAsyncErrorHandler], if this response isn't a success.
     */
    private suspend fun HttpResponse.orThrow(what: String): HttpResponse {
        if (status.isSuccess()) return this
        val error = bodyAsError()
        log.e { "Failed to $what: $error" }
        throw error.toThrowable().also(GlobalAsyncErrorHandler::setError)
    }

    private suspend fun <T> HttpResponse.decode(serializer: KSerializer<T>): T = json.decodeFromString(serializer, bodyAsText())

    /** Applies [transform] to the locally cached [departmentId], if it's synced at all. */
    private suspend fun patchDepartment(departmentId: Uuid, transform: (Department) -> Department) {
        val department = departmentsRepository.get(departmentId) ?: return
        departmentsRepository.update(transform(department))
    }

    /** Applies [transform] to whichever locally cached department owns [qualificationId], if any is synced. */
    private suspend fun patchDepartmentOwning(qualificationId: Uuid, transform: (Department) -> Department) {
        val department = departmentsRepository.selectAll().find { department ->
            department.qualifications.orEmpty().any { it.id == qualificationId }
        } ?: return
        departmentsRepository.update(transform(department))
    }

    suspend fun create(departmentId: Uuid, name: String, description: String?): Qualification {
        val created = httpClient.post("/departments/$departmentId/qualifications") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(CreateQualificationRequest.serializer(), CreateQualificationRequest(name, description)))
        }.orThrow("create qualification").decode(Qualification.serializer())

        patchDepartment(departmentId) { it.copy(qualifications = it.qualifications.orEmpty() + created) }
        return created
    }

    /** Only the given fields change: a `null` [name] or [description] is left as it is, and a blank [description] clears it. */
    suspend fun update(id: Uuid, name: String? = null, description: String? = null): Qualification {
        val updated = httpClient.patch("/qualifications/$id") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdateQualificationRequest.serializer(), UpdateQualificationRequest(name, description)))
        }.orThrow("update qualification").decode(Qualification.serializer())

        patchDepartment(updated.departmentId) { department ->
            department.copy(qualifications = department.qualifications.orEmpty().map { if (it.id == id) updated else it })
        }
        return updated
    }

    /** Also deletes every grant of it. The server refuses while an event still requires it. */
    suspend fun delete(id: Uuid) {
        httpClient.delete("/qualifications/$id").orThrow("delete qualification")

        patchDepartmentOwning(id) { department ->
            department.copy(
                qualifications = department.qualifications.orEmpty().filterNot { it.id == id },
                qualificationGrants = department.qualificationGrants.orEmpty().filterNot { it.qualificationId == id },
            )
        }
    }

    /** Grants [qualificationId] to [userSub], replacing their existing grant if they already hold it. */
    suspend fun grant(qualificationId: Uuid, userSub: String, expiresAt: Instant? = null): QualificationGrant {
        val grant = httpClient.post("/qualifications/$qualificationId/grants") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(GrantQualificationRequest.serializer(), GrantQualificationRequest(userSub, expiresAt)))
        }.orThrow("grant qualification").decode(QualificationGrant.serializer())

        patchDepartmentOwning(qualificationId) { department ->
            val withoutExisting = department.qualificationGrants.orEmpty().filterNot { it.qualificationId == qualificationId && it.userSub == userSub }
            department.copy(qualificationGrants = withoutExisting + grant)
        }
        return grant
    }

    suspend fun revoke(qualificationId: Uuid, userSub: String) {
        httpClient.delete("/qualifications/$qualificationId/grants/$userSub").orThrow("revoke qualification")

        patchDepartmentOwning(qualificationId) { department ->
            department.copy(qualificationGrants = department.qualificationGrants.orEmpty().filterNot { it.qualificationId == qualificationId && it.userSub == userSub })
        }
    }
}
