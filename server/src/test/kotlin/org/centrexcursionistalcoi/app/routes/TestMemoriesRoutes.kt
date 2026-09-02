package org.centrexcursionistalcoi.app.routes

import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertBody
import org.centrexcursionistalcoi.app.assertError
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.data.Memory
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.database.entity.InventoryItemEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity
import org.centrexcursionistalcoi.app.database.entity.LendingEntity
import org.centrexcursionistalcoi.app.database.entity.MemoryEntity
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.request.UpdateMemoryRequest
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.test.FakeUser2
import org.centrexcursionistalcoi.app.test.LoginType
import org.centrexcursionistalcoi.app.utils.toUUID
import org.centrexcursionistalcoi.app.utils.toUUIDOrNull
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.uuid.toJavaUuid

class TestMemoriesRoutes : ApplicationTestBase() {

    private val exampleItemTypeId = "8e5b8c53-df8c-4e0a-9f9d-2a0f5c1a6a3a".toUUID()
    private val exampleItemId = "1a9f6bda-53f0-4f38-9c9e-3f4e4f9c8b1c".toUUID()

    @Test
    fun test_create_memory_notLoggedIn() = ProvidedRouteTests.test_notLoggedIn_form("/memories")

    @Test
    fun test_create_memory_missingText() = runApplicationTest(shouldLogIn = LoginType.USER) {
        client.submitFormWithBinaryData("/memories", formData { append("place", "Somewhere") }).apply {
            assertError(Error.MemoryNotGiven())
        }
    }

    @Test
    fun test_create_memory_standalone() = runApplicationTest(shouldLogIn = LoginType.USER) {
        val location = client.submitFormWithBinaryData(
            "/memories",
            formData {
                append("text", "A memory with no lending attached")
                append("place", "Alcoi")
                append("sport", Sports.HIKING.name)
            }
        ).run {
            assertStatusCode(HttpStatusCode.Created)
            val location = headers[HttpHeaders.Location]
            assertNotNull(location, "Missing Location header in response")
            location
        }

        client.get(location).apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(Memory.serializer()) { memory ->
                assertEquals("A memory with no lending attached", memory.text)
                assertEquals("Alcoi", memory.place)
                assertEquals(Sports.HIKING, memory.sport)
                assertNull(memory.lending, "Standalone memory should not be linked to a lending")
                assertNotNull(memory.pdf, "A summary PDF should have been generated")
            }
        }
    }

    @Test
    fun test_create_memory_forLending() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            val itemType = InventoryItemTypeEntity.new(exampleItemTypeId) { displayName = "Item Type" }
            val item = InventoryItemEntity.new(exampleItemId) { type = itemType }
            val user = FakeUser.provideEntity()
            val lending = LendingEntity.new {
                this.userSub = user
                this.from = LocalDate.of(2025, 10, 8)
                this.to = LocalDate.of(2025, 10, 9)
                this.returned = true
            }
            item to lending
        },
    ) { context ->
        val (_, lending) = context.dibResult!!

        client.submitFormWithBinaryData(
            "/memories",
            formData {
                append("text", "Everything went great")
                append("lending", lending.id.value.toString())
            }
        ).apply {
            assertStatusCode(HttpStatusCode.Created)
        }

        // The lending should now expose its memory's id, and be marked as submitted
        val memoryId = client.get("/inventory/lendings/${lending.id.value}").run {
            assertStatusCode(HttpStatusCode.OK)
            var memoryId: kotlin.uuid.Uuid? = null
            assertBody(Lending.serializer()) { fetchedLending ->
                assertEquals(true, fetchedLending.memorySubmitted)
                memoryId = fetchedLending.memory
                assertNotNull(memoryId, "Lending should expose its memory's id")
            }
            memoryId!!
        }

        // The memory itself should be fetchable and contain the submitted content
        client.get("/memories/$memoryId").apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(Memory.serializer()) { memory ->
                assertEquals("Everything went great", memory.text)
                assertEquals(lending.id.value.toString(), memory.lending.toString())
            }
        }

        // Submitting a second memory for the same lending must be rejected
        client.submitFormWithBinaryData(
            "/memories",
            formData {
                append("text", "A second memory")
                append("lending", lending.id.value.toString())
            }
        ).apply {
            assertError(Error.MemoryAlreadySubmitted())
        }
    }

    @Test
    fun test_get_memory_permissionDenied() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            val otherUser = FakeUser2.provideEntity()
            val lending = LendingEntity.new {
                this.userSub = otherUser
                this.from = LocalDate.of(2025, 10, 8)
                this.to = LocalDate.of(2025, 10, 9)
                this.returned = true
            }
            MemoryEntity.new {
                this.text = "Not yours to see"
                this.lending = lending
            }
        },
    ) { context ->
        val memory = context.dibResult!!

        client.get("/memories/${memory.id.value}").apply {
            assertError(Error.PermissionRejected())
        }
    }

    @Test
    fun test_patch_memory() = runApplicationTest(shouldLogIn = LoginType.USER) {
        val location = client.submitFormWithBinaryData(
            "/memories",
            formData { append("text", "Original text") }
        ).run {
            assertStatusCode(HttpStatusCode.Created)
            headers[HttpHeaders.Location]!!
        }
        val memoryId = location.substringAfterLast('/').toUUIDOrNull()
        assertNotNull(memoryId)

        client.patch(location) {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdateMemoryRequest.serializer(), UpdateMemoryRequest(place = "Updated place")))
        }.apply {
            assertStatusCode(HttpStatusCode.NoContent)
        }

        client.get(location).apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(Memory.serializer()) { memory ->
                assertEquals("Updated place", memory.place)
                assertEquals(memoryId.toString(), memory.id.toJavaUuid().toString())
            }
        }
    }
}
