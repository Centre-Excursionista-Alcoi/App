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
import org.centrexcursionistalcoi.app.assertError
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.data.DepartmentRole
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.data.Post
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.database.entity.PostEntity
import org.centrexcursionistalcoi.app.database.table.DepartmentMembers
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.request.CreatePostRequest
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.test.LoginType
import org.jetbrains.exposed.v1.jdbc.insert
import kotlin.uuid.toKotlinUuid

/**
 * `POST /posts` accepting a JSON body (#659), alongside the existing multipart path (still covered by
 * `TestRoutes.kt`'s generic `runTestsOnRoute` harness, unaffected by any of this). Posts is the pilot entity for
 * the migration described in #659 -- see `RoutesBase.kt`'s `createRequestSerializer`/`jsonCreator` and
 * `PostsRoutes.kt`.
 */
class TestPostsJsonCreation : ApplicationTestBase() {
    private suspend fun HttpClient.postJson(request: CreatePostRequest) = post("/posts") {
        contentType(ContentType.Application.Json)
        setBody(json.encodeToString(CreatePostRequest.serializer(), request))
    }

    @Test
    fun test_create_json_notLoggedIn_unauthorized() = runApplicationTest {
        client.postJson(CreatePostRequest(title = "Title", content = "Content")).assertStatusCode(HttpStatusCode.Unauthorized)
    }

    @Test
    fun test_create_json_loggedIn_notAdmin_noRole_forbidden() = runApplicationTest(
        shouldLogIn = LoginType.USER,
    ) {
        client.postJson(CreatePostRequest(title = "Title", content = "Content")).assertStatusCode(HttpStatusCode.Forbidden)
    }

    @Test
    fun test_create_json_malformedBody_badRequest() = runApplicationTest(shouldLogIn = LoginType.ADMIN) {
        client.post("/posts") {
            contentType(ContentType.Application.Json)
            setBody("not json")
        }.assertStatusCode(HttpStatusCode.BadRequest)
    }

    @Test
    fun test_create_json_missingRequiredField_badRequest() = runApplicationTest(shouldLogIn = LoginType.ADMIN) {
        client.post("/posts") {
            contentType(ContentType.Application.Json)
            // "title" is required by CreatePostRequest and has no default -- decoding this must fail.
            setBody("""{"content":"Content"}""")
        }.assertStatusCode(HttpStatusCode.BadRequest)
    }

    @Test
    fun test_create_json_requiredFieldsOnly_persistsAndIsReadableBack() = runApplicationTest(shouldLogIn = LoginType.ADMIN) {
        val location = client.postJson(CreatePostRequest(title = "JSON Post", content = "Created via JSON")).run {
            assertStatusCode(HttpStatusCode.Created)
            val location = headers[HttpHeaders.Location]
            assertNotNull(location)
            location
        }

        client.get(location).apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(Post.serializer()) { post ->
                assertEquals("JSON Post", post.title)
                assertEquals("Created via JSON", post.content)
                assertNull(post.department)
                assertNull(post.link)
            }
        }
    }

    @Test
    fun test_create_json_withDepartmentLinkAndFile_persistsEverything() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = { DepartmentEntity.new { displayName = "JSON Department" } },
    ) { context ->
        val department = context.dibResult!!
        val fileBytes = "%PDF-1.4 fake content".encodeToByteArray()

        val location = client.postJson(
            CreatePostRequest(
                title = "JSON Post With Extras",
                content = "Body",
                department = department.id.value.toKotlinUuid(),
                link = "https://example.com",
                files = listOf(FileWithContext(bytes = fileBytes, name = "doc.pdf", contentType = ContentType.Application.Pdf)),
            )
        ).run {
            assertStatusCode(HttpStatusCode.Created)
            val location = headers[HttpHeaders.Location]
            assertNotNull(location)
            location
        }

        client.get(location).apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(Post.serializer()) { post ->
                assertEquals("JSON Post With Extras", post.title)
                assertEquals("https://example.com", post.link)
                assertEquals(department.id.value.toString(), post.department.toString())
                assertEquals(1, post.files.size)
            }
        }

        val storedFile = Database { FileEntity.all().first() }
        assertEquals("doc.pdf", storedFile.name)
        val storedBytes = Database { storedFile.bytes }
        assertEquals(fileBytes.toList(), storedBytes.toList())
    }

    @Test
    fun test_create_json_contentManager_otherDepartment_rejectedAndDoesNotOrphanFile() = runApplicationTest(
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
            CreatePostRequest(
                title = "Cross-department JSON post",
                content = "Body",
                department = otherDepartment.id.value.toKotlinUuid(),
                files = listOf(FileWithContext(bytes = "data".encodeToByteArray(), name = "f.pdf")),
            )
        ).assertError(Error.PermissionRejected())

        // Neither the post nor the file it referenced (created before the department could be authorized) may
        // survive the rejection -- same guarantee the multipart path already has, now for the JSON path too.
        val remainingPosts = Database { PostEntity.all().toList() }
        val remainingFiles = Database { FileEntity.all().toList() }
        assertEquals(0, remainingPosts.size, "Rejected post creation should have been rolled back")
        assertEquals(0, remainingFiles.size, "Rejected post creation should not leave an orphaned file behind")
    }

    @Test
    fun test_create_multipartStillWorks_alongsideJson() = runApplicationTest(shouldLogIn = LoginType.ADMIN) {
        client.post("/posts") {
            // No body/content-type at all: neither multipart nor JSON -- must still be rejected the same way
            // it always was, not silently accepted as an empty JSON create.
        }.assertStatusCode(HttpStatusCode.BadRequest)
    }
}
