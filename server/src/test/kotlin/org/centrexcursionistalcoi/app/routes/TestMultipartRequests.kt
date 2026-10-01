package org.centrexcursionistalcoi.app.routes

import io.ktor.client.plugins.resources.patch
import kotlin.test.assertNull
import kotlin.time.Clock
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.escapeIfNeeded
import io.ktor.server.testing.ApplicationTestBuilder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.KSerializer
import org.centrexcursionistalcoi.app.ADMIN_GROUP_NAME
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.ResourcesUtils
import org.centrexcursionistalcoi.app.assertError
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.EventEntity
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity
import org.centrexcursionistalcoi.app.database.entity.MemoryEntity
import org.centrexcursionistalcoi.app.database.entity.PostEntity
import org.centrexcursionistalcoi.app.database.table.Memories
import org.centrexcursionistalcoi.app.database.table.MemoriesFiles
import org.centrexcursionistalcoi.app.database.table.PostFiles
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.request.CreateDepartmentRequest
import org.centrexcursionistalcoi.app.request.CreateEventRequest
import org.centrexcursionistalcoi.app.request.CreateInventoryItemTypeRequest
import org.centrexcursionistalcoi.app.request.CreatePostRequest
import org.centrexcursionistalcoi.app.request.RequestWithFiles
import org.centrexcursionistalcoi.app.request.UpdateDepartmentRequest
import org.centrexcursionistalcoi.app.request.UpdateEventRequest
import org.centrexcursionistalcoi.app.request.UpdateMemoryRequest
import org.centrexcursionistalcoi.app.request.UpdatePostRequest
import org.centrexcursionistalcoi.app.storage.testStorage
import org.centrexcursionistalcoi.app.test.FakeAdminUser
import org.centrexcursionistalcoi.app.test.LoginType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll

/**
 * Tests requests with files sent as multipart, the way the app sends them (see [RequestWithFiles]).
 */
class TestMultipartRequests : ApplicationTestBase() {
    private val png = ResourcesUtils.bytesFromResource("/square.png")
    private val pdf = ResourcesUtils.bytesFromResource("/document.pdf")

    /**
     * The body the app sends for [request]: its JSON first, then [files] (part name to name and contents).
     */
    private fun <T> multipart(request: T, serializer: KSerializer<T>, files: Map<String, Pair<String, ByteArray>>) =
        MultiPartFormDataContent(
            formData {
                append(
                    RequestWithFiles.REQUEST_PART,
                    json.encodeToString(serializer, request),
                    Headers.build { append(HttpHeaders.ContentType, ContentType.Application.Json.toString()) },
                )
                for ((part, file) in files) {
                    val (name, bytes) = file
                    append(part, bytes, Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=${name.escapeIfNeeded()}")
                    })
                }
            }
        )

    private fun partFile(part: String, name: String? = null, id: Uuid? = null) = FileWithContext(name = name, part = part, id = id)

    private suspend fun ApplicationTestBuilder.postMultipart(path: String, body: MultiPartFormDataContent): HttpResponse =
        client.post(path) { setBody(body) }

    private suspend fun ApplicationTestBuilder.patchMultipart(path: String, body: MultiPartFormDataContent): HttpResponse =
        client.patch(path) { setBody(body) }

    private fun uploadTempFiles(): List<String> =
        Files.list(Path.of(System.getProperty("java.io.tmpdir"))).use { files ->
            files.map { it.fileName.toString() }.filter { it.startsWith("cea-upload-") }.toList()
        }

    private fun createdId(response: HttpResponse): Uuid =
        Uuid.parse(response.headers[HttpHeaders.Location]!!.substringAfterLast('/'))

    @Test
    fun test_create_department() = runApplicationTest(shouldLogIn = LoginType.ADMIN) {
        val before = uploadTempFiles()
        val response = postMultipart(
            "/departments",
            multipart(CreateDepartmentRequest("Department", image = partFile("file_0")), CreateDepartmentRequest.serializer(), mapOf("file_0" to ("logo.png" to png))),
        )
        response.assertStatusCode(HttpStatusCode.Created)

        val image = Database { DepartmentEntity[createdId(response)].image!!.let { FileEntity[it.id] } }
        assertEquals("logo.png", image.name)
        assertEquals(ContentType.Image.PNG, image.contentType)
        assertContentEquals(png, testStorage.readBytes(image.objectKey))
        assertEquals(before, uploadTempFiles())
    }

    @Test
    fun test_create_event_and_inventoryType() = runApplicationTest(shouldLogIn = LoginType.ADMIN) {
        val event = postMultipart(
            "/events",
            multipart(
                CreateEventRequest(kotlin.time.Clock.System.now(), "Place", "Event", image = partFile("file_0", name = "event.png")),
                CreateEventRequest.serializer(),
                mapOf("file_0" to ("ignored.png" to png)),
            ),
        ).also { it.assertStatusCode(HttpStatusCode.Created) }
        val type = postMultipart(
            "/inventory/types",
            multipart(
                CreateInventoryItemTypeRequest("Type", image = partFile("file_0")),
                CreateInventoryItemTypeRequest.serializer(),
                mapOf("file_0" to ("type.png" to png)),
            ),
        ).also { it.assertStatusCode(HttpStatusCode.Created) }

        Database {
            val eventImage = EventEntity[createdId(event)].image!!
            // The name in the request wins over the part's file name
            assertEquals("event.png", eventImage.name)
            assertContentEquals(png, eventImage.readBytes())
            assertContentEquals(png, InventoryItemTypeEntity[createdId(type)].image!!.readBytes())
        }
    }

    @Test
    fun test_create_post_withSeveralFiles() = runApplicationTest(shouldLogIn = LoginType.ADMIN) {
        val response = postMultipart(
            "/posts",
            multipart(
                CreatePostRequest("Post", "Content", files = listOf(partFile("file_0"), partFile("file_1"))),
                CreatePostRequest.serializer(),
                mapOf("file_0" to ("a.png" to png), "file_1" to ("b.pdf" to pdf)),
            ),
        )
        response.assertStatusCode(HttpStatusCode.Created)

        val files = Database { PostEntity[createdId(response)].files.map { it.name to it.readBytes() }.toMap() }
        assertContentEquals(png, files.getValue("a.png"))
        assertContentEquals(pdf, files.getValue("b.pdf"))
    }

    @Test
    fun test_create_missingPart() = runApplicationTest(shouldLogIn = LoginType.ADMIN) {
        postMultipart(
            "/departments",
            multipart(CreateDepartmentRequest("Department", image = partFile("file_1")), CreateDepartmentRequest.serializer(), mapOf("file_0" to ("logo.png" to png))),
        ).assertError(Error.MalformedRequest())

        assertEquals(0, Database { DepartmentEntity.count() })
        assertTrue(testStorage.keys().isEmpty())
    }

    @Test
    fun test_create_withoutRequestPart_rejected() = runApplicationTest(shouldLogIn = LoginType.ADMIN) {
        // Fields as parts of their own, with no "request" part
        val body = MultiPartFormDataContent(
            formData {
                append("displayName", "Department")
                append("image", png, Headers.build { append(HttpHeaders.ContentDisposition, "filename=logo.png") })
            }
        )
        postMultipart("/departments", body).assertError(Error.MalformedRequest())

        assertEquals(0, Database { DepartmentEntity.count() })
        assertTrue(testStorage.keys().isEmpty())
    }

    @Test
    fun test_patch_department_replacesImageKeepingItsId() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            val image = FileEntity.create(byteArrayOf(1, 2, 3), "old.bin")
            DepartmentEntity.new { displayName = "Department"; this.image = image }.id.value to image.id.value
        },
    ) { context ->
        val (departmentId, imageId) = context.dibResult!!
        val oldKey = Database { FileEntity[imageId].objectKey }

        patchMultipart(
            "/departments/$departmentId",
            multipart(
                UpdateDepartmentRequest(image = partFile("file_0", id = imageId)),
                UpdateDepartmentRequest.serializer(),
                mapOf("file_0" to ("new.png" to png)),
            ),
        ).assertStatusCode(HttpStatusCode.OK)

        val image = Database { FileEntity[DepartmentEntity[departmentId].image!!.id] }
        assertEquals(imageId, image.id.value)
        assertNotEquals(oldKey, image.objectKey)
        assertEquals("new.png", image.name)
        assertEquals(ContentType.Image.PNG, image.contentType)
        assertContentEquals(png, testStorage.readBytes(image.objectKey))
        assertEquals(listOf(image.objectKey), testStorage.keys(), "The old contents must be deleted")
    }

    @Test
    fun test_patch_event_newImage() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            EventEntity.new { start = Clock.System.now(); place = "Place"; title = "Event" }.id.value
        },
    ) { context ->
        patchMultipart(
            "/events/${context.dibResult}",
            multipart(UpdateEventRequest(image = partFile("file_0")), UpdateEventRequest.serializer(), mapOf("file_0" to ("event.png" to png))),
        ).assertStatusCode(HttpStatusCode.OK)

        assertContentEquals(png, Database { EventEntity[context.dibResult!!].image!!.readBytes() })
    }

    @Test
    fun test_patch_post_addsFileAndRemovesAnother() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            val old = FileEntity.create(byteArrayOf(1, 2, 3), "old.bin")
            val post = PostEntity.new { title = "Post"; content = "Content" }
            PostFiles.insert { it[PostFiles.post] = post.id; it[file] = old.id }
            post.id.value to old.id.value
        },
    ) { context ->
        val (postId, oldFile) = context.dibResult!!
        patchMultipart(
            "/posts/$postId",
            multipart(
                UpdatePostRequest(files = listOf(partFile("file_0"), FileWithContext(id = oldFile))),
                UpdatePostRequest.serializer(),
                mapOf("file_0" to ("new.pdf" to pdf)),
            ),
        ).assertStatusCode(HttpStatusCode.OK)

        val files = Database { PostFiles.selectAll().where { PostFiles.post eq postId }.map { FileEntity[it[PostFiles.file]] } }
        val file = files.single()
        assertEquals("new.pdf", Database { file.name })
        assertContentEquals(pdf, Database { file.readBytes() })
        assertEquals(listOf(Database { file.objectKey }), testStorage.keys())
    }

    @Test
    fun test_patch_memory_addsAttachment() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            Memories.insertAndGetId {
                it[text] = "Memory"
                it[submittedBy] = FakeAdminUser.provideEntity().id
                it[fromInstant] = Clock.System.now()
                it[fromZone] = "Europe/Madrid"
                it[toInstant] = Clock.System.now()
                it[toZone] = "Europe/Madrid"
            }.value
        },
    ) { context ->
        val memoryId = context.dibResult!!
        patchMultipart(
            "/memories/$memoryId",
            multipart(UpdateMemoryRequest(attachments = listOf(partFile("file_0"))), UpdateMemoryRequest.serializer(), mapOf("file_0" to ("photo.png" to png))),
        ).assertStatusCode(HttpStatusCode.NoContent)

        Database {
            val attachment = MemoriesFiles.selectAll().where { MemoriesFiles.memory eq memoryId }.single()[MemoriesFiles.file]
                .let { FileEntity[it] }
            assertEquals("photo.png", attachment.name)
            assertContentEquals(png, attachment.readBytes())
            // Restricted like the attachments given on creation
            assertEquals(listOf(ADMIN_GROUP_NAME), attachment.rules?.readGroups)
            assertNotNull(MemoryEntity[memoryId].pdf, "The PDF must be regenerated")
        }
    }

    @Test
    fun test_patch_missingPart() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = { DepartmentEntity.new { displayName = "Department" }.id.value },
    ) { context ->
        val before = uploadTempFiles()
        patchMultipart(
            "/departments/${context.dibResult}",
            multipart(UpdateDepartmentRequest(image = partFile("file_1")), UpdateDepartmentRequest.serializer(), mapOf("file_0" to ("logo.png" to png))),
        ).assertError(Error.MalformedRequest())

        assertEquals(null, Database { DepartmentEntity[context.dibResult!!].image })
        assertTrue(testStorage.keys().isEmpty())
        assertEquals(before, uploadTempFiles())
    }

    @Test
    fun test_patch_requestPartMustComeFirst() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = { DepartmentEntity.new { displayName = "Department" }.id.value },
    ) { context ->
        val body = MultiPartFormDataContent(
            formData {
                append("file_0", png, Headers.build { append(HttpHeaders.ContentDisposition, "filename=logo.png") })
                append(RequestWithFiles.REQUEST_PART, json.encodeToString(UpdateDepartmentRequest.serializer(), UpdateDepartmentRequest(image = partFile("file_0"))))
            }
        )
        patchMultipart("/departments/${context.dibResult}", body).assertError(Error.MalformedRequest())
    }

    @Test
    fun test_patch_undecodableRequest() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = { DepartmentEntity.new { displayName = "Department" }.id.value },
    ) { context ->
        val body = MultiPartFormDataContent(
            formData {
                append(RequestWithFiles.REQUEST_PART, "{not json")
                append("file_0", png, Headers.build { append(HttpHeaders.ContentDisposition, "filename=logo.png") })
            }
        )
        patchMultipart("/departments/${context.dibResult}", body).assertError(Error.MalformedRequest())
        postMultipart("/departments", body).assertError(Error.MalformedRequest())
        assertTrue(testStorage.keys().isEmpty())
    }

    @Test
    fun test_patch_duplicatePartNames() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = { PostEntity.new { title = "Post"; content = "Content" }.id.value },
    ) { context ->
        val body = MultiPartFormDataContent(
            formData {
                append(RequestWithFiles.REQUEST_PART, json.encodeToString(UpdatePostRequest.serializer(), UpdatePostRequest(files = listOf(partFile("file_0")))))
                append("file_0", png, Headers.build { append(HttpHeaders.ContentDisposition, "filename=a.png") })
                append("file_0", pdf, Headers.build { append(HttpHeaders.ContentDisposition, "filename=b.pdf") })
            }
        )
        patchMultipart("/posts/${context.dibResult}", body).assertError(Error.MalformedRequest())
        assertTrue(testStorage.keys().isEmpty())
    }

    @Test
    fun test_patch_unknownEntity() = runApplicationTest(shouldLogIn = LoginType.ADMIN) {
        client.patch(Api.Departments.Id("${Uuid.random()}")) {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }.assertStatusCode(HttpStatusCode.NotFound)
    }

    @Test
    fun test_patch_withoutContentType() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = { DepartmentEntity.new { displayName = "Department" }.id.value },
    ) { context ->
        client.patch(Api.Departments.Id("${context.dibResult}")).assertStatusCode(HttpStatusCode.BadRequest)
    }

    @Test
    fun test_patch_json_withContents_rejected() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = { DepartmentEntity.new { displayName = "Department" }.id.value },
    ) { context ->
        // The contents of the file encoded as Base64 in the JSON, instead of in a part of its own
        client.patch(Api.Departments.Id("${context.dibResult}")) {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdateDepartmentRequest.serializer(), UpdateDepartmentRequest(image = FileWithContext(png, name = "logo.png"))))
        }.assertError(Error.MalformedRequest())

        assertNull(Database { DepartmentEntity[context.dibResult!!].image })
        assertTrue(testStorage.keys().isEmpty())
    }
}
