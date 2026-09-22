package org.centrexcursionistalcoi.app.routes

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.uuid.toKotlinUuid
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertBody
import org.centrexcursionistalcoi.app.assertError
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.data.DepartmentRole
import org.centrexcursionistalcoi.app.data.Event
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.EventEntity
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.database.entity.QualificationEntity
import org.centrexcursionistalcoi.app.database.table.DepartmentMembers
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.request.CreateEventRequest
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.test.LoginType
import org.jetbrains.exposed.v1.jdbc.insert

/**
 * `POST /events` accepting a JSON body (#659), same pattern as `TestPostsJsonCreation`/`TestInventoryJsonCreation`.
 * The multipart path (including its exhaustive qualification-requirements edge cases) stays covered by
 * `TestRoutes.kt` and `TestEventQualificationRequirements.kt`, unaffected by any of this.
 */
class TestEventsJsonCreation : ApplicationTestBase() {
    private val future = Clock.System.now().plus(7.days)

    private suspend fun HttpClient.postJson(request: CreateEventRequest) = post("/events") {
        contentType(ContentType.Application.Json)
        setBody(json.encodeToString(CreateEventRequest.serializer(), request))
    }

    @Test
    fun test_create_json_notLoggedIn_unauthorized() = runApplicationTest {
        client.postJson(CreateEventRequest(start = future, place = "Somewhere", title = "Event")).assertStatusCode(HttpStatusCode.Unauthorized)
    }

    @Test
    fun test_create_json_loggedIn_noRole_forbidden() = runApplicationTest(shouldLogIn = LoginType.USER) {
        client.postJson(CreateEventRequest(start = future, place = "Somewhere", title = "Event")).assertStatusCode(HttpStatusCode.Forbidden)
    }

    @Test
    fun test_create_json_malformedBody_badRequest() = runApplicationTest(shouldLogIn = LoginType.ADMIN) {
        client.post("/events") {
            contentType(ContentType.Application.Json)
            setBody("not json")
        }.assertStatusCode(HttpStatusCode.BadRequest)
    }

    @Test
    fun test_create_json_missingRequiredField_badRequest() = runApplicationTest(shouldLogIn = LoginType.ADMIN) {
        client.post("/events") {
            contentType(ContentType.Application.Json)
            setBody("""{"place":"Somewhere","title":"Event"}""")
        }.assertStatusCode(HttpStatusCode.BadRequest)
    }

    @Test
    fun test_create_json_requiredFieldsOnly_defaultsAreCorrect() = runApplicationTest(shouldLogIn = LoginType.ADMIN) {
        val location = client.postJson(CreateEventRequest(start = future, place = "Somewhere", title = "JSON Event")).run {
            assertStatusCode(HttpStatusCode.Created)
            val location = headers[HttpHeaders.Location]
            assertNotNull(location)
            location
        }

        client.get(location).apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(Event.serializer()) { event ->
                assertEquals("JSON Event", event.title)
                assertFalse(event.requiresConfirmation)
                assertFalse(event.requiresInsurance)
                assertEquals(emptyList(), event.qualificationRequirements)
            }
        }
    }

    // Regression: the multipart creator never read requiresInsurance at all (see EventsRoutes.kt), even though
    // the client already sent it and PATCH already supported it -- silently ignored on create. Fixed as part of
    // building the JSON creator (#659); this is the test for that fix specifically.
    @Test
    fun test_create_json_requiresInsurance_isActuallyPersisted() = runApplicationTest(shouldLogIn = LoginType.ADMIN) {
        val location = client.postJson(
            CreateEventRequest(start = future, place = "Somewhere", title = "Needs insurance", requiresInsurance = true, requiresConfirmation = true)
        ).run {
            assertStatusCode(HttpStatusCode.Created)
            val location = headers[HttpHeaders.Location]
            assertNotNull(location)
            location
        }

        client.get(location).apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(Event.serializer()) { event ->
                assertTrue(event.requiresInsurance)
                assertTrue(event.requiresConfirmation)
            }
        }
    }

    @Test
    fun test_create_json_withDepartmentAndImage_persistsEverything() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = { DepartmentEntity.new { displayName = "JSON Department" } },
    ) { context ->
        val department = context.dibResult!!
        val imageBytes = "fake png bytes".encodeToByteArray()

        val location = client.postJson(
            CreateEventRequest(
                start = future,
                place = "Somewhere",
                title = "Event with extras",
                description = "A description",
                maxPeople = 20L,
                department = department.id.value.toKotlinUuid(),
                image = FileWithContext(bytes = imageBytes, name = "event.png", contentType = ContentType.Image.PNG),
            )
        ).run {
            assertStatusCode(HttpStatusCode.Created)
            val location = headers[HttpHeaders.Location]
            assertNotNull(location)
            location
        }

        client.get(location).apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(Event.serializer()) { event ->
                assertEquals("A description", event.description)
                assertEquals(20L, event.maxPeople)
                assertEquals(department.id.value.toString(), event.department.toString())
                assertNotNull(event.image)
            }
        }

        val storedImage = Database { FileEntity.all().first() }
        val storedBytes = Database { storedImage.bytes }
        assertEquals(imageBytes.toList(), storedBytes.toList())
    }

    @Test
    fun test_create_json_requirementsWithoutDepartment_rejected() = runApplicationTest(shouldLogIn = LoginType.ADMIN) {
        client.postJson(
            CreateEventRequest(
                start = future,
                place = "Somewhere",
                title = "Bad requirements",
                qualificationRequirements = listOf(listOf(kotlin.uuid.Uuid.random())),
            )
        ).assertStatusCode(HttpStatusCode.BadRequest)

        assertEquals(0, Database { EventEntity.all().count() })
    }

    @Test
    fun test_create_json_withValidRequirements_storesThem() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            val department = DepartmentEntity.new { displayName = "Climbing Department" }
            QualificationEntity.new { this.department = department; name = "Basic" }
        },
    ) { context ->
        val qualification = context.dibResult!!
        val department = Database { qualification.department!! }

        val location = client.postJson(
            CreateEventRequest(
                start = future,
                place = "Crag",
                title = "Climbing day",
                department = department.id.value.toKotlinUuid(),
                qualificationRequirements = listOf(listOf(qualification.id.value.toKotlinUuid())),
            )
        ).run {
            assertStatusCode(HttpStatusCode.Created)
            val location = headers[HttpHeaders.Location]
            assertNotNull(location)
            location
        }

        client.get(location).apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(Event.serializer()) { event ->
                assertEquals(listOf(listOf(qualification.id.value.toKotlinUuid())), event.qualificationRequirements)
            }
        }
    }

    @Test
    fun test_create_json_contentManager_otherDepartment_rejectedAndDoesNotOrphanImage() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            FakeUser.provideEntity()
            val managed = DepartmentEntity.new { displayName = "Managed Department" }
            val other = DepartmentEntity.new { displayName = "Other Department" }
            DepartmentMembers.insert {
                it[userSub] = FakeUser.SUB
                it[departmentId] = managed.id
                it[confirmed] = true
                it[roles] = listOf(DepartmentRole.CONTENT_MANAGER.storageName)
            }
            other
        },
    ) { context ->
        val otherDepartment = context.dibResult!!

        client.postJson(
            CreateEventRequest(
                start = future,
                place = "Somewhere",
                title = "Cross-department event",
                department = otherDepartment.id.value.toKotlinUuid(),
                image = FileWithContext(bytes = "img".encodeToByteArray(), name = "x.png"),
            )
        ).assertError(Error.PermissionRejected())

        val remainingEvents = Database { EventEntity.all().toList() }
        val remainingFiles = Database { FileEntity.all().toList() }
        assertEquals(0, remainingEvents.size, "Rejected event creation should have been rolled back")
        assertEquals(0, remainingFiles.size, "Rejected event creation should not leave an orphaned image behind")
    }
}
