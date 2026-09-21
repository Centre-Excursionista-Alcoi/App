package org.centrexcursionistalcoi.app.network

import com.diamondedge.logging.logging
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.GlobalAsyncErrorHandler
import org.centrexcursionistalcoi.app.data.DepartmentRosterMember
import org.centrexcursionistalcoi.app.data.Qualification
import org.centrexcursionistalcoi.app.data.QualificationGrant
import org.centrexcursionistalcoi.app.error.bodyAsError
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.request.CreateQualificationRequest
import org.centrexcursionistalcoi.app.request.GrantQualificationRequest
import org.centrexcursionistalcoi.app.request.UpdateQualificationRequest
import org.koin.core.annotation.Singleton

/**
 * Talks to the server's qualification routes. Qualifications aren't synced into the local database (yet): the
 * server's routes for them are plain request/response ones, without the `lastUpdate` bookkeeping the synced
 * entities have, so every call here hits the network.
 */
@Singleton
class QualificationsRemoteRepository {
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

    /** Every qualification definition. Readable by any logged-in user. */
    suspend fun list(): List<Qualification> =
        httpClient.get("/qualifications")
            .orThrow("list qualifications")
            .decode(ListSerializer(Qualification.serializer()))

    suspend fun create(departmentId: Uuid, name: String, description: String?): Qualification =
        httpClient.post("/departments/$departmentId/qualifications") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(CreateQualificationRequest.serializer(), CreateQualificationRequest(name, description)))
        }.orThrow("create qualification").decode(Qualification.serializer())

    /** Only the given fields change: a `null` [name] or [description] is left as it is, and a blank [description] clears it. */
    suspend fun update(id: Uuid, name: String? = null, description: String? = null): Qualification =
        httpClient.patch("/qualifications/$id") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdateQualificationRequest.serializer(), UpdateQualificationRequest(name, description)))
        }.orThrow("update qualification").decode(Qualification.serializer())

    /** Also deletes every grant of it. The server refuses while an event still requires it. */
    suspend fun delete(id: Uuid) {
        httpClient.delete("/qualifications/$id").orThrow("delete qualification")
    }

    /** Who holds [qualificationId], expired grants included. Only for the department's examiners, managers and people managers. */
    suspend fun grants(qualificationId: Uuid): List<QualificationGrant> =
        httpClient.get("/qualifications/$qualificationId/grants")
            .orThrow("list qualification grants")
            .decode(ListSerializer(QualificationGrant.serializer()))

    /** Grants [qualificationId] to [userSub], replacing their existing grant if they already hold it. */
    suspend fun grant(qualificationId: Uuid, userSub: String, expiresAt: Instant? = null): QualificationGrant =
        httpClient.post("/qualifications/$qualificationId/grants") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(GrantQualificationRequest.serializer(), GrantQualificationRequest(userSub, expiresAt)))
        }.orThrow("grant qualification").decode(QualificationGrant.serializer())

    suspend fun revoke(qualificationId: Uuid, userSub: String) {
        httpClient.delete("/qualifications/$qualificationId/grants/$userSub").orThrow("revoke qualification")
    }

    /** The logged-in user's own grants, expired ones included. */
    suspend fun myGrants(): List<QualificationGrant> =
        httpClient.get("/profile/qualifications")
            .orThrow("list own qualifications")
            .decode(ListSerializer(QualificationGrant.serializer()))

    /** The confirmed members of [departmentId] an examiner can grant to, optionally narrowed to names containing [query]. */
    suspend fun roster(departmentId: Uuid, query: String? = null, limit: Int? = null): List<DepartmentRosterMember> =
        httpClient.get("/departments/$departmentId/roster") {
            query?.takeIf { it.isNotBlank() }?.let { parameter("q", it) }
            limit?.let { parameter("limit", it) }
        }.orThrow("list department roster").decode(ListSerializer(DepartmentRosterMember.serializer()))
}
