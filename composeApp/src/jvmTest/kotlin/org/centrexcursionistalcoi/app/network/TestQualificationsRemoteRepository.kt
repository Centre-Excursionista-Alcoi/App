package org.centrexcursionistalcoi.app.network

import androidx.room3.Room
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.DepartmentRosterMember
import org.centrexcursionistalcoi.app.data.Qualification
import org.centrexcursionistalcoi.app.data.QualificationGrant
import org.centrexcursionistalcoi.app.database.AppDatabase
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.getRoomDatabase
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.request.CreateQualificationRequest
import org.centrexcursionistalcoi.app.request.GrantQualificationRequest
import org.centrexcursionistalcoi.app.request.UpdateQualificationRequest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * The client's calls must match the server's qualification routes: paths, verbs, bodies and error handling --
 * and each mutation must patch the affected department's locally synced copy from the response, without a
 * separate re-fetch (see the class KDoc on [QualificationsRemoteRepository] itself).
 */
class TestQualificationsRemoteRepository {
    private val departmentId = Uuid.random()
    private val qualificationId = Uuid.random()

    private val qualification = Qualification(qualificationId, departmentId, "Lead climbing", "Leads sport routes")
    private val grant = QualificationGrant(qualificationId, "sub-1", "examiner", Instant.fromEpochMilliseconds(1_000), null)

    private val requests = mutableListOf<HttpRequestData>()
    private val original = _httpClient
    private var db: AppDatabase? = null
    private lateinit var departmentsRepository: DepartmentsRepository

    @AfterTest
    fun tearDown() {
        _httpClient = original
        db?.close()
    }

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK): HttpResponseData =
        respond(body, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))

    /**
     * [seed] is the department to have locally cached before the call under test, or `null` to test the
     * not-synced-locally case. Its default already includes [qualification] and no grants, matching what a
     * synced department normally looks like for [update]/[delete]/[grant]/[revoke] tests; [create] tests pass
     * a seed without it, since that's the one being created.
     */
    private suspend fun repository(
        seed: Department? = Department(departmentId, "Test Department", members = null, qualifications = listOf(qualification), qualificationGrants = emptyList()),
        handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): QualificationsRemoteRepository {
        _httpClient = HttpClient(MockEngine { request ->
            requests += request
            handler(request)
        })
        val database = getRoomDatabase(Room.inMemoryDatabaseBuilder<AppDatabase>(), Dispatchers.IO)
        db = database
        departmentsRepository = DepartmentsRepository(database)
        if (seed != null) departmentsRepository.insert(seed)
        return QualificationsRemoteRepository(departmentsRepository)
    }

    private fun <T> encode(serializer: KSerializer<T>, value: T) = json.encodeToString(serializer, value)

    private fun HttpRequestData.bodyText() = (body as TextContent).text

    private val HttpRequestData.path get() = url.encodedPath

    @Test
    fun create_postsJsonToTheDepartment_andAddsItLocally() = runTest {
        val repository = repository(seed = Department(departmentId, "Test Department", members = null)) {
            json(encode(Qualification.serializer(), qualification), HttpStatusCode.Created)
        }

        assertEquals(qualification, repository.create(departmentId, "Lead climbing", "Leads sport routes"))

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/departments/$departmentId/qualifications", request.path)
        assertEquals(ContentType.Application.Json, request.body.contentType?.withoutParameters())
        assertEquals(CreateQualificationRequest("Lead climbing", "Leads sport routes"), json.decodeFromString(CreateQualificationRequest.serializer(), request.bodyText()))

        assertEquals(listOf(qualification), departmentsRepository.get(departmentId)?.qualifications)
    }

    @Test
    fun create_whenDepartmentNotSyncedLocally_stillReturnsIt_justSkipsThePatch() = runTest {
        val repository = repository(seed = null) { json(encode(Qualification.serializer(), qualification), HttpStatusCode.Created) }

        assertEquals(qualification, repository.create(departmentId, "Lead climbing", "Leads sport routes"))
        assertNull(departmentsRepository.get(departmentId))
    }

    @Test
    fun update_patchesOnlyTheGivenFields_andReplacesItLocally() = runTest {
        val renamed = qualification.copy(name = "Renamed")
        val repository = repository { json(encode(Qualification.serializer(), renamed)) }

        repository.update(qualificationId, name = "Renamed")

        val request = requests.single()
        assertEquals(HttpMethod.Patch, request.method)
        assertEquals("/qualifications/$qualificationId", request.path)
        assertEquals(UpdateQualificationRequest(name = "Renamed"), json.decodeFromString(UpdateQualificationRequest.serializer(), request.bodyText()))

        assertEquals(listOf(renamed), departmentsRepository.get(departmentId)?.qualifications)
    }

    @Test
    fun delete_removesItLocally_alongWithItsGrants() = runTest {
        val repository = repository(
            seed = Department(departmentId, "Test Department", members = null, qualifications = listOf(qualification), qualificationGrants = listOf(grant)),
        ) { respond("", HttpStatusCode.NoContent) }

        repository.delete(qualificationId)

        assertEquals(HttpMethod.Delete, requests.single().method)
        assertEquals("/qualifications/$qualificationId", requests.single().path)

        val department = departmentsRepository.get(departmentId)
        assertEquals(emptyList(), department?.qualifications)
        assertEquals(emptyList(), department?.qualificationGrants)
    }

    @Test
    fun delete_and_revoke_useTheRightPaths() = runTest {
        val repository = repository(
            seed = Department(departmentId, "Test Department", members = null, qualifications = listOf(qualification), qualificationGrants = listOf(grant)),
        ) { respond("", HttpStatusCode.NoContent) }

        repository.delete(qualificationId)
        repository.revoke(qualificationId, "sub-1")

        assertEquals(listOf(HttpMethod.Delete, HttpMethod.Delete), requests.map { it.method })
        assertEquals("/qualifications/$qualificationId", requests[0].path)
        assertEquals("/qualifications/$qualificationId/grants/sub-1", requests[1].path)
    }

    @Test
    fun grant_sendsTheUserAndExpiry_andUpsertsItLocally() = runTest {
        val repository = repository { json(encode(QualificationGrant.serializer(), grant)) }
        val expiresAt = Instant.fromEpochMilliseconds(9_000_000)

        assertEquals(grant, repository.grant(qualificationId, "sub-1", expiresAt))

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/qualifications/$qualificationId/grants", request.path)
        assertEquals(GrantQualificationRequest("sub-1", expiresAt), json.decodeFromString(GrantQualificationRequest.serializer(), request.bodyText()))

        assertEquals(listOf(grant), departmentsRepository.get(departmentId)?.qualificationGrants)
    }

    @Test
    fun grant_again_replacesTheExistingGrantLocally_notDuplicatesIt() = runTest {
        val existing = grant.copy(expiresAt = Instant.fromEpochMilliseconds(1))
        val renewed = grant.copy(expiresAt = Instant.fromEpochMilliseconds(9_000_000))
        val repository = repository(
            seed = Department(departmentId, "Test Department", members = null, qualifications = listOf(qualification), qualificationGrants = listOf(existing)),
        ) { json(encode(QualificationGrant.serializer(), renewed)) }

        repository.grant(qualificationId, "sub-1", renewed.expiresAt)

        assertEquals(listOf(renewed), departmentsRepository.get(departmentId)?.qualificationGrants)
    }

    @Test
    fun revoke_removesTheGrantLocally() = runTest {
        val repository = repository(
            seed = Department(departmentId, "Test Department", members = null, qualifications = listOf(qualification), qualificationGrants = listOf(grant)),
        ) { respond("", HttpStatusCode.NoContent) }

        repository.revoke(qualificationId, "sub-1")

        assertEquals(emptyList(), departmentsRepository.get(departmentId)?.qualificationGrants)
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
    fun serverErrors_areThrown_notSwallowed_andNothingIsPatchedLocally() = runTest {
        val error = Error.QualificationAlreadyExists()
        val repository = repository(seed = Department(departmentId, "Test Department", members = null)) {
            json(encode(Error.serializer(), error), error.statusCode)
        }

        val thrown = assertFailsWith<ServerException> { repository.create(departmentId, "Lead climbing", null) }
        assertEquals(HttpStatusCode.Conflict.value, thrown.responseStatusCode)
        assertTrue(departmentsRepository.get(departmentId)?.qualifications.orEmpty().isEmpty())
    }
}
