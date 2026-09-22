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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertBody
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.DepartmentRole
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.database.table.DepartmentMembers
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.request.CreateDepartmentRequest
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.test.LoginType
import org.jetbrains.exposed.v1.jdbc.insert

/**
 * `POST /departments` accepting a JSON body (#659), same pattern as the other three entities. The multipart path
 * stays covered by `TestRoutes.kt`'s generic `runTestsOnRoute` harness, unaffected by any of this.
 */
class TestDepartmentJsonCreation : ApplicationTestBase() {
    private suspend fun HttpClient.postJson(request: CreateDepartmentRequest) = post("/departments") {
        contentType(ContentType.Application.Json)
        setBody(json.encodeToString(CreateDepartmentRequest.serializer(), request))
    }

    @Test
    fun test_create_json_notLoggedIn_unauthorized() = runApplicationTest {
        client.postJson(CreateDepartmentRequest(displayName = "Department")).assertStatusCode(HttpStatusCode.Unauthorized)
    }

    @Test
    fun test_create_json_malformedBody_badRequest() = runApplicationTest(shouldLogIn = LoginType.ADMIN) {
        client.post("/departments") {
            contentType(ContentType.Application.Json)
            setBody("not json")
        }.assertStatusCode(HttpStatusCode.BadRequest)
    }

    @Test
    fun test_create_json_missingRequiredField_badRequest() = runApplicationTest(shouldLogIn = LoginType.ADMIN) {
        client.post("/departments") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }.assertStatusCode(HttpStatusCode.BadRequest)
    }

    @Test
    fun test_create_json_requiredFieldOnly_persistsAndIsReadableBack() = runApplicationTest(shouldLogIn = LoginType.ADMIN) {
        val location = client.postJson(CreateDepartmentRequest(displayName = "JSON Department")).run {
            assertStatusCode(HttpStatusCode.Created)
            val location = headers[HttpHeaders.Location]
            assertNotNull(location)
            location
        }

        client.get(location).apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(Department.serializer()) { department ->
                assertEquals("JSON Department", department.displayName)
                assertNull(department.image)
            }
        }
    }

    @Test
    fun test_create_json_withImage_persistsIt() = runApplicationTest(shouldLogIn = LoginType.ADMIN) {
        val imageBytes = "fake png bytes".encodeToByteArray()

        val location = client.postJson(
            CreateDepartmentRequest(
                displayName = "Department With Image",
                image = FileWithContext(bytes = imageBytes, name = "dept.png", contentType = ContentType.Image.PNG),
            )
        ).run {
            assertStatusCode(HttpStatusCode.Created)
            val location = headers[HttpHeaders.Location]
            assertNotNull(location)
            location
        }

        client.get(location).apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(Department.serializer()) { department ->
                assertNotNull(department.image)
            }
        }

        val storedImage = Database { FileEntity.all().first() }
        val storedBytes = Database { storedImage.bytes }
        assertEquals(imageBytes.toList(), storedBytes.toList())
    }

    // Creating a department is always global-admin-only, even for a DepartmentRole.ADMIN of an existing
    // department: the fine-grained check resolves the *new* department's own id, which can't possibly have a
    // DepartmentMembers row yet for anyone (see EntityWritePermission's departmentOfEntity comment in
    // DepartmentRoutes.kt). This is the JSON path's version of that same guarantee.
    @Test
    fun test_create_json_departmentAdmin_ofAnotherDepartment_isStillRejected_andDoesNotOrphanImage() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            FakeUser.provideEntity()
            val managed = DepartmentEntity.new { displayName = "Managed Department" }
            DepartmentMembers.insert {
                it[userSub] = FakeUser.SUB
                it[departmentId] = managed.id
                it[confirmed] = true
                it[roles] = listOf(DepartmentRole.ADMIN.storageName)
            }
        },
    ) {
        client.postJson(
            CreateDepartmentRequest(
                displayName = "New department",
                image = FileWithContext(bytes = "img".encodeToByteArray(), name = "x.png"),
            )
        ).assertStatusCode(HttpStatusCode.Forbidden)

        val remainingDepartments = Database { DepartmentEntity.all().toList() }
        val remainingFiles = Database { FileEntity.all().toList() }
        // Two departments existed before this request (the one seeded above): only that one may remain.
        assertEquals(1, remainingDepartments.size, "Rejected department creation should have been rolled back")
        assertEquals(0, remainingFiles.size, "Rejected department creation should not leave an orphaned image behind")
    }
}
