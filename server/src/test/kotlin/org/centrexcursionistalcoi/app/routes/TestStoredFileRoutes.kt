package org.centrexcursionistalcoi.app.routes

import io.ktor.http.Headers
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.forms.formData
import io.ktor.client.request.basicAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.ResourcesUtils
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.data.Post
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.EventEntity
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity
import org.centrexcursionistalcoi.app.database.entity.MemoryEntity
import org.centrexcursionistalcoi.app.database.entity.PostEntity
import org.centrexcursionistalcoi.app.database.entity.UserInsuranceEntity
import org.centrexcursionistalcoi.app.database.table.Memories
import org.centrexcursionistalcoi.app.database.table.MemoriesFiles
import org.centrexcursionistalcoi.app.database.table.PostFiles
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.request.UpdatePostRequest
import org.centrexcursionistalcoi.app.security.Passwords
import org.centrexcursionistalcoi.app.storage.createTestFile
import org.centrexcursionistalcoi.app.storage.testStorage
import org.centrexcursionistalcoi.app.test.FakeAdminUser
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.test.LoginType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.centrexcursionistalcoi.app.security.AuthTokens
import org.centrexcursionistalcoi.app.database.table.AuthSessionMethod
import org.centrexcursionistalcoi.app.security.ClientInfo
import io.ktor.client.request.header

/**
 * Tests that routes serve, create and delete the contents of files in the file storage.
 */
class TestStoredFileRoutes : ApplicationTestBase() {
    private val png = ResourcesUtils.bytesFromResource("/square.png")

    private fun JdbcTransaction.newFile(bytes: ByteArray = png): FileEntity = FileEntity.create(bytes, "file")

    private fun storedKeys(): Set<String> = testStorage.keys().toSet()

    private fun keysOfFiles(): Set<String> = Database { FileEntity.all().map { it.objectKey } }.toSet()

    @Test
    fun test_download_streamsContents() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { newFile().id.value },
    ) { context ->
        val response = client.get("/download/${context.dibResult}")

        response.assertStatusCode(HttpStatusCode.OK)
        assertEquals(png.size.toString(), response.headers[HttpHeaders.ContentLength])
        assertNotNull(response.headers[HttpHeaders.LastModified])
        assertEquals(ContentType.Image.PNG.toString(), response.headers[HttpHeaders.ContentType])
        assertContentEquals(png, response.bodyAsBytes())
    }

    @Test
    fun test_download_missingContents() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { newFile().id.value },
    ) { context ->
        testStorage.objects.clear()

        client.get("/download/${context.dibResult}").assertStatusCode(HttpStatusCode.InternalServerError)
    }

    @Test
    fun test_posts_haveNoContents() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            val file = newFile()
            PostEntity.new { title = "Post"; content = "Content" }.also { post ->
                PostFiles.insert { it[PostFiles.post] = post.id; it[PostFiles.file] = file.id }
            }.id.value to file.id.value
        },
    ) { context ->
        val (_, fileId) = context.dibResult!!
        val body = client.get("/posts").also { it.assertStatusCode(HttpStatusCode.OK) }.bodyAsText()

        val file = json.parseToJsonElement(body).jsonArray.single().jsonObject["files"]!!.jsonArray.single().jsonObject
        assertEquals(fileId.toString(), file["id"]!!.jsonPrimitive.content)
        // Clients from before contents were moved to the storage require "bytes"
        assertEquals("", file["bytes"]!!.jsonPrimitive.content)
        assertFalse("objectKey" in file, "The key of the contents must not be sent")
        assertEquals(png.size.toLong(), file["size"]!!.jsonPrimitive.content.toLong())

        // Clients decode it
        val posts = json.decodeFromString(ListSerializer(Post.serializer()), body)
        assertEquals(fileId.toString(), posts.single().files.single().id.toString())

        // Newer clients decode files without "bytes"
        val withoutBytes = JsonObject(file - "bytes")
        assertEquals(0, json.decodeFromJsonElement(FileWithContext.serializer(), withoutBytes).bytes.size)
    }

    @Test
    fun test_patchPost_linksNewFiles() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = { PostEntity.new { title = "Post"; content = "Content" }.id.value },
    ) { context ->
        val postId = context.dibResult!!
        client.patch("/posts/$postId") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdatePostRequest.serializer(), UpdatePostRequest(files = listOf(FileWithContext(png, name = "new.png")))))
        }.assertStatusCode(HttpStatusCode.OK)

        val fileIds = Database { PostFiles.selectAll().where { PostFiles.post eq postId }.map { it[PostFiles.file].value } }
        val file = Database { FileEntity[fileIds.single()] }
        assertContentEquals(png, file.readBytes())
    }

    @Test
    fun test_patchPost_cannotDeleteOtherFiles() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            PostEntity.new { title = "Post"; content = "Content" }.id.value to newFile().id.value
        },
    ) { context ->
        val (postId, otherFile) = context.dibResult!!
        // An empty file means "remove it from the post": only the post's own files can be removed
        client.patch("/posts/$postId") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdatePostRequest.serializer(), UpdatePostRequest(files = listOf(FileWithContext(id = kotlin.uuid.Uuid.parse(otherFile.toString()))))))
        }

        assertNotNull(Database { FileEntity.findById(otherFile) })
        assertEquals(1, storedKeys().size)
    }

    @Test
    fun test_deleteEntities_deletesTheirFiles() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            val department = DepartmentEntity.new { displayName = "Department"; image = newFile() }
            val type = InventoryItemTypeEntity.new { displayName = "Type"; image = newFile() }
            val event = EventEntity.new {
                start = Instant.now(); place = "Place"; title = "Event"; image = newFile()
            }
            val post = PostEntity.new { title = "Post"; content = "Content" }
            listOf(newFile(), newFile()).forEach { file ->
                PostFiles.insert { it[PostFiles.post] = post.id; it[PostFiles.file] = file.id }
            }
            // Stays: nothing deletes it
            newFile()
            listOf("departments/${department.id.value}", "inventory/types/${type.id.value}", "events/${event.id.value}", "posts/${post.id.value}")
        },
    ) { context ->
        assertEquals(6, storedKeys().size)

        for (path in context.dibResult!!) {
            client.delete("/$path").assertStatusCode(HttpStatusCode.NoContent)
        }

        assertEquals(1, Database { FileEntity.count() })
        assertEquals(keysOfFiles(), storedKeys())
    }

    @Test
    fun test_deleteMemory_deletesItsFiles() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = { newMemory(FakeUser.provideEntity().id.value) },
    ) { context ->
        assertEquals(2, storedKeys().size)

        client.delete("/memories/${context.dibResult}").assertStatusCode(HttpStatusCode.NoContent)

        assertEquals(0, Database { FileEntity.count() })
        assertTrue(storedKeys().isEmpty())
    }

    private fun JdbcTransaction.newMemory(submittedBy: String): UUID {
        val memory = Memories.insertAndGetId {
            it[text] = "Memory"
            it[Memories.submittedBy] = submittedBy
            it[fromInstant] = Instant.now()
            it[fromZone] = "Europe/Madrid"
            it[toInstant] = Instant.now()
            it[toZone] = "Europe/Madrid"
            it[pdf] = newFile().id
        }
        MemoriesFiles.insert { it[MemoriesFiles.memory] = memory; it[file] = newFile().id }
        return memory.value
    }

    @Test
    fun test_deleteAccount_deletesFiles() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            val user = FakeUser.provideEntity()
            newMemory(user.sub.value)
            UserInsuranceEntity.new {
                userSub = user
                insuranceCompany = "Company"
                policyNumber = "1234"
                validFrom = LocalDate.of(2025, 1, 1)
                validTo = LocalDate.of(2025, 12, 31)
            }.addDocuments(listOf(newFile(), newFile()))
        },
    ) {
        assertEquals(4, storedKeys().size)

        client.post("/delete_account").assertStatusCode(HttpStatusCode.NoContent)

        assertEquals(0, Database { MemoryEntity.count() })
        assertEquals(0, Database { FileEntity.count() })
        assertTrue(storedKeys().isEmpty())
    }

    @Test
    fun test_webDav_servesContents() = runApplicationTest(
        databaseInitBlock = {
            FakeAdminUser.provideEntity().password = Passwords.hash(PASSWORD.toCharArray())
            DepartmentEntity.new { displayName = "Department"; image = newFile() }.id.value
        },
    ) { context ->
        val path = "/webdav/Departments/${context.dibResult}"

        client.get(path) { basicAuth(FakeAdminUser.EMAIL, PASSWORD) }.let { response ->
            response.assertStatusCode(HttpStatusCode.OK)
            assertEquals(png.size.toString(), response.headers[HttpHeaders.ContentLength])
            assertContentEquals(png, response.bodyAsBytes())
        }
        client.head(path) { basicAuth(FakeAdminUser.EMAIL, PASSWORD) }.let { response ->
            response.assertStatusCode(HttpStatusCode.OK)
            assertEquals(png.size.toString(), response.headers[HttpHeaders.ContentLength])
        }
        client.request("/webdav/Departments/") {
            method = HttpMethod("PROPFIND")
            basicAuth(FakeAdminUser.EMAIL, PASSWORD)
        }.let { response ->
            assertTrue(response.bodyAsText().contains("<D:getcontentlength>${png.size}</D:getcontentlength>"), response.bodyAsText())
        }
    }

    private fun uploadTempFiles(): List<String> =
        java.nio.file.Files.list(java.nio.file.Path.of(System.getProperty("java.io.tmpdir"))).use { files ->
            files.map { it.fileName.toString() }.filter { it.startsWith("cea-upload-") }.toList()
        }

    private fun insuranceForm(document: ByteArray, policyNumber: String?) = formData {
        append("insuranceCompany", "Company")
        policyNumber?.let { append("policyNumber", it) }
        append("validFrom", "2025-01-01")
        append("validTo", "2025-12-31")
        append("document", document, Headers.build {
            append(HttpHeaders.ContentType, ContentType.Application.Pdf.toString())
            append(HttpHeaders.ContentDisposition, "filename=\"policy.pdf\"")
        })
    }

    @Test
    fun test_upload_largeFile_isStoredIntact() = runApplicationTest(shouldLogIn = LoginType.USER) {
        val before = uploadTempFiles()
        val document = ResourcesUtils.bytesFromResource("/document.pdf") + kotlin.random.Random.nextBytes(24 * 1024 * 1024)

        // The test client logs whole bodies: send this one without logging
        val token = Database {
            AuthTokens.startSession(FakeUser.provideEntity(), AuthSessionMethod.PASSWORD, ClientInfo(null, "test"))
        }.accessToken
        createClient { }.submitFormWithBinaryData("/profile/insurances", insuranceForm(document, "1234")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.assertStatusCode(HttpStatusCode.NoContent)

        val file = Database { UserInsuranceEntity.all().single().documentIds().single().let { FileEntity[it] } }
        assertEquals(document.size.toLong(), file.size)
        assertEquals(ContentType.Application.Pdf, file.contentType)
        assertEquals("policy.pdf", file.name)
        assertContentEquals(document, testStorage.readBytes(file.objectKey))
        assertEquals(before, uploadTempFiles(), "The temporary file of the upload must be deleted")
    }

    @Test
    fun test_upload_rejected_deletesTemporaryFiles() = runApplicationTest(shouldLogIn = LoginType.USER) {
        val before = uploadTempFiles()

        // Rejected after receiving the document, before storing it
        client.submitFormWithBinaryData("/profile/insurances", insuranceForm(byteArrayOf(1, 2, 3), policyNumber = null))
            .assertStatusCode(HttpStatusCode.BadRequest)

        assertEquals(before, uploadTempFiles(), "The temporary file of the upload must be deleted")
        assertTrue(storedKeys().isEmpty())
    }

    companion object {
        private const val PASSWORD = "TestPassword123"
    }
}
