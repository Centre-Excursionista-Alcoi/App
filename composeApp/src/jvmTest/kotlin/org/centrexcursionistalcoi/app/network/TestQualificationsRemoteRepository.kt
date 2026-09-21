package org.centrexcursionistalcoi.app.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.data.DepartmentRosterMember
import org.centrexcursionistalcoi.app.data.Qualification
import org.centrexcursionistalcoi.app.data.QualificationGrant
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.request.CreateQualificationRequest
import org.centrexcursionistalcoi.app.request.GrantQualificationRequest
import org.centrexcursionistalcoi.app.request.UpdateQualificationRequest

/** The client's calls must match the server's qualification routes: paths, verbs, bodies and error handling. */
class TestQualificationsRemoteRepository {
    private val departmentId = Uuid.random()
    private val qualificationId = Uuid.random()

    private val qualification = Qualification(qualificationId, departmentId, "Lead climbing", "Leads sport routes")
    private val grant = QualificationGrant(qualificationId, "sub-1", "examiner", Instant.fromEpochMilliseconds(1_000), null)

    private val requests = mutableListOf<HttpRequestData>()
    private val original = _httpClient

    @AfterTest
    fun tearDown() {
        _httpClient = original
    }

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK): HttpResponseData =
        respond(body, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))

    private fun repository(handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): QualificationsRemoteRepository {
        _httpClient = HttpClient(MockEngine { request ->
            requests += request
            handler(request)
        })
        return QualificationsRemoteRepository()
    }

    private fun <T> encode(serializer: KSerializer<T>, value: T) = json.encodeToString(serializer, value)

    private fun HttpRequestData.bodyText() = (body as TextContent).text

    private val HttpRequestData.path get() = url.encodedPath

    @Test
    fun list_allAndByDepartment() = runTest {
        val repository = repository { json(encode(ListSerializer(Qualification.serializer()), listOf(qualification))) }

        assertEquals(listOf(qualification), repository.list())
        assertEquals(listOf(qualification), repository.list(departmentId))

        assertEquals(HttpMethod.Get, requests[0].method)
        assertEquals("/qualifications", requests[0].path)
        assertNull(requests[0].url.parameters["department"])
        assertEquals(departmentId.toString(), requests[1].url.parameters["department"])
    }

    @Test
    fun create_postsJsonToTheDepartment() = runTest {
        val repository = repository { json(encode(Qualification.serializer(), qualification), HttpStatusCode.Created) }

        assertEquals(qualification, repository.create(departmentId, "Lead climbing", "Leads sport routes"))

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/departments/$departmentId/qualifications", request.path)
        assertEquals(ContentType.Application.Json, request.body.contentType?.withoutParameters())
        assertEquals(CreateQualificationRequest("Lead climbing", "Leads sport routes"), json.decodeFromString(CreateQualificationRequest.serializer(), request.bodyText()))
    }

    @Test
    fun update_patchesOnlyTheGivenFields() = runTest {
        val repository = repository { json(encode(Qualification.serializer(), qualification)) }

        repository.update(qualificationId, name = "Renamed")

        val request = requests.single()
        assertEquals(HttpMethod.Patch, request.method)
        assertEquals("/qualifications/$qualificationId", request.path)
        assertEquals(UpdateQualificationRequest(name = "Renamed"), json.decodeFromString(UpdateQualificationRequest.serializer(), request.bodyText()))
    }

    @Test
    fun delete_and_revoke_useTheRightPaths() = runTest {
        val repository = repository { respond("", HttpStatusCode.NoContent) }

        repository.delete(qualificationId)
        repository.revoke(qualificationId, "sub-1")

        assertEquals(listOf(HttpMethod.Delete, HttpMethod.Delete), requests.map { it.method })
        assertEquals("/qualifications/$qualificationId", requests[0].path)
        assertEquals("/qualifications/$qualificationId/grants/sub-1", requests[1].path)
    }

    @Test
    fun grants_and_myGrants() = runTest {
        val repository = repository { json(encode(ListSerializer(QualificationGrant.serializer()), listOf(grant))) }

        assertEquals(listOf(grant), repository.grants(qualificationId))
        assertEquals(listOf(grant), repository.myGrants())

        assertEquals("/qualifications/$qualificationId/grants", requests[0].path)
        assertEquals("/profile/qualifications", requests[1].path)
    }

    @Test
    fun grant_sendsTheUserAndExpiry() = runTest {
        val repository = repository { json(encode(QualificationGrant.serializer(), grant)) }
        val expiresAt = Instant.fromEpochMilliseconds(9_000_000)

        assertEquals(grant, repository.grant(qualificationId, "sub-1", expiresAt))
        repository.grant(qualificationId, "sub-2")

        val request = requests[0]
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/qualifications/$qualificationId/grants", request.path)
        assertEquals(GrantQualificationRequest("sub-1", expiresAt), json.decodeFromString(GrantQualificationRequest.serializer(), request.bodyText()))
        assertNull(json.decodeFromString(GrantQualificationRequest.serializer(), requests[1].bodyText()).expiresAt)
    }

    @Test
    fun roster_sendsTheSearchOnlyWhenThereIsOne() = runTest {
        val member = DepartmentRosterMember("sub-1", "Ada Lovelace")
        val repository = repository { json(encode(ListSerializer(DepartmentRosterMember.serializer()), listOf(member))) }

        assertEquals(listOf(member), repository.roster(departmentId, query = "ada", limit = 20))
        repository.roster(departmentId, query = "  ")

        assertEquals("/departments/$departmentId/roster", requests[0].path)
        assertEquals("ada", requests[0].url.parameters["q"])
        assertEquals("20", requests[0].url.parameters["limit"])
        assertNull(requests[1].url.parameters["q"])
        assertNull(requests[1].url.parameters["limit"])
    }

    @Test
    fun serverErrors_areThrown_notSwallowed() = runTest {
        val error = Error.QualificationAlreadyExists()
        val repository = repository { json(encode(Error.serializer(), error), error.statusCode) }

        val thrown = assertFailsWith<ServerException> { repository.create(departmentId, "Lead climbing", null) }
        assertEquals(HttpStatusCode.Conflict.value, thrown.responseStatusCode)
    }
}
