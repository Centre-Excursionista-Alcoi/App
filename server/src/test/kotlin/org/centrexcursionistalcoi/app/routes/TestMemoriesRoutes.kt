package org.centrexcursionistalcoi.app.routes

import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.datetime.LocalTime
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toKotlinLocalDate
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertBody
import org.centrexcursionistalcoi.app.assertError
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.data.DepartmentRole
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.data.Memory
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.data.ZonedDateTime
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity
import org.centrexcursionistalcoi.app.database.entity.LendingEntity
import org.centrexcursionistalcoi.app.database.entity.MemoryEntity
import org.centrexcursionistalcoi.app.database.table.DepartmentMembers
import org.centrexcursionistalcoi.app.database.table.Memories
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.request.UpdateMemoryRequest
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.test.FakeUser2
import org.centrexcursionistalcoi.app.test.LoginType
import org.centrexcursionistalcoi.app.utils.toUUID
import org.centrexcursionistalcoi.app.utils.toUUIDOrNull
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.SizedCollection
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.uuid.toJavaUuid
import kotlinx.datetime.LocalDate as KotlinLocalDate

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
    fun test_create_memory_standalone_missingDateRange() = runApplicationTest(shouldLogIn = LoginType.USER) {
        client.submitFormWithBinaryData("/memories", formData { append("text", "No date range given") }).apply {
            assertError(Error.MissingArgument("from"))
        }
    }

    @Test
    fun test_create_memory_standalone() = runApplicationTest(shouldLogIn = LoginType.USER) {
        val zone = TimeZone.currentSystemDefault()
        val from = ZonedDateTime(zone, KotlinLocalDate(2025, 6, 15), LocalTime(10, 0, 0))
        val to = ZonedDateTime(zone, KotlinLocalDate(2025, 6, 15), LocalTime(12, 0, 0))

        val location = client.submitFormWithBinaryData(
            "/memories",
            formData {
                append("text", "A memory with no lending attached")
                append("place", "Alcoi")
                append("sport", Sports.HIKING.name)
                append("from", from.toString())
                append("to", to.toString())
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
                assertEquals(from, memory.from)
                assertEquals(to, memory.to)
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

        // The memory itself should be fetchable and contain the submitted content, with its date range taken
        // automatically from the lending's own from/to.
        client.get("/memories/$memoryId").apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(Memory.serializer()) { memory ->
                assertEquals("Everything went great", memory.text)
                assertEquals(lending.id.value.toString(), memory.lending.toString())
                val (lendingFrom, lendingTo) = Database { lending.from to lending.to }
                val zone = TimeZone.currentSystemDefault()
                assertEquals(
                    ZonedDateTime(zone, lendingFrom.toKotlinLocalDate(), LocalTime(0, 0, 0)),
                    memory.from,
                )
                assertEquals(
                    ZonedDateTime(zone, lendingTo.toKotlinLocalDate(), LocalTime(23, 59, 59)),
                    memory.to,
                )
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
                this.submittedBy = otherUser
                this.from = ZonedDateTime.fromInstant(Clock.System.now(), TimeZone.currentSystemDefault())
                this.to = ZonedDateTime.fromInstant(Clock.System.now(), TimeZone.currentSystemDefault())
            }
        },
    ) { context ->
        val memory = context.dibResult!!

        client.get("/memories/${memory.id.value}").apply {
            assertError(Error.PermissionRejected())
        }
    }

    @Test
    fun test_get_memory_taggedMemberCanReadButNotModify() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            val submitter = FakeUser2.provideEntity()
            // The logged-in user (FakeUser) is tagged as a participating member, but didn't submit the memory
            val taggedMember = FakeUser.provideMemberEntity()
            MemoryEntity.new {
                this.text = "A shared activity"
                this.submittedBy = submitter
                this.members = SizedCollection(listOf(taggedMember))
                this.from = ZonedDateTime.fromInstant(Clock.System.now(), TimeZone.currentSystemDefault())
                this.to = ZonedDateTime.fromInstant(Clock.System.now(), TimeZone.currentSystemDefault())
            }
        },
    ) { context ->
        val memory = context.dibResult!!

        // The tagged member can see the memory in the list...
        client.get("/memories").apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(ListSerializer(Memory.serializer())) { memories ->
                assertTrue(memories.any { it.id.toJavaUuid() == memory.id.value }, "Tagged memory should be in the list")
            }
        }

        // ...and fetch it directly...
        client.get("/memories/${memory.id.value}").apply {
            assertStatusCode(HttpStatusCode.OK)
        }

        // ...but still cannot modify it, since they are not the submitter nor an admin
        client.patch("/memories/${memory.id.value}") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdateMemoryRequest.serializer(), UpdateMemoryRequest(place = "Nice try")))
        }.apply {
            assertError(Error.PermissionRejected())
        }
    }

    @Test
    fun test_get_and_patch_memory_departmentMemoryManager() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            FakeUser.provideEntity()
            val submitter = FakeUser2.provideEntity()
            val department = DepartmentEntity.new { displayName = "Test Department" }

            // The logged-in user (FakeUser) holds MEMORY_MANAGER in the memory's department, but didn't submit it
            // and isn't tagged as a participant.
            DepartmentMembers.insert {
                it[DepartmentMembers.userSub] = FakeUser.SUB
                it[DepartmentMembers.departmentId] = department.id
                it[DepartmentMembers.confirmed] = true
                it[DepartmentMembers.roles] = listOf(DepartmentRole.MEMORY_MANAGER.storageName)
            }

            MemoryEntity.new {
                this.text = "A department activity"
                this.submittedBy = submitter
                this.department = department
                this.from = ZonedDateTime.fromInstant(Clock.System.now(), TimeZone.currentSystemDefault())
                this.to = ZonedDateTime.fromInstant(Clock.System.now(), TimeZone.currentSystemDefault())
            }
        },
    ) { context ->
        val memory = context.dibResult!!

        // The department's memory manager sees the memory in the list...
        client.get("/memories").apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(ListSerializer(Memory.serializer())) { memories ->
                assertTrue(memories.any { it.id.toJavaUuid() == memory.id.value }, "Managed department's memory should be in the list")
            }
        }

        // ...can fetch it directly...
        client.get("/memories/${memory.id.value}").apply {
            assertStatusCode(HttpStatusCode.OK)
        }

        // ...and can modify it, unlike a mere tagged member.
        client.patch("/memories/${memory.id.value}") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdateMemoryRequest.serializer(), UpdateMemoryRequest(place = "Updated by manager")))
        }.apply {
            assertStatusCode(HttpStatusCode.NoContent)
        }
    }

    @Test
    fun test_patch_memory() = runApplicationTest(shouldLogIn = LoginType.USER) {
        val location = client.submitFormWithBinaryData(
            "/memories",
            formData {
                append("text", "Original text")
                append("from", ZonedDateTime(TimeZone.currentSystemDefault(), KotlinLocalDate(2025, 6, 15), LocalTime(10, 0, 0)).toString())
                append("to", ZonedDateTime(TimeZone.currentSystemDefault(), KotlinLocalDate(2025, 6, 15), LocalTime(12, 0, 0)).toString())
            }
        ).run {
            assertStatusCode(HttpStatusCode.Created)
            headers[HttpHeaders.Location]!!
        }
        val memoryId = location.substringAfterLast('/').toUUIDOrNull()
        assertNotNull(memoryId)

        val originalPdfId = client.get(location).run {
            assertStatusCode(HttpStatusCode.OK)
            var pdfId: kotlin.uuid.Uuid? = null
            assertBody(Memory.serializer()) { memory ->
                pdfId = memory.pdf
                assertNotNull(pdfId, "A summary PDF should have been generated on creation")
            }
            pdfId!!
        }
        val originalPdfBytes = client.get("/download/$originalPdfId").run {
            assertStatusCode(HttpStatusCode.OK)
            bodyAsBytes()
        }

        client.patch(location) {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdateMemoryRequest.serializer(), UpdateMemoryRequest(place = "Updated place")))
        }.apply {
            assertStatusCode(HttpStatusCode.NoContent)
        }

        val newPdfId = client.get(location).run {
            assertStatusCode(HttpStatusCode.OK)
            var pdfId: kotlin.uuid.Uuid? = null
            assertBody(Memory.serializer()) { memory ->
                assertEquals("Updated place", memory.place)
                assertEquals(memoryId.toString(), memory.id.toJavaUuid().toString())
                pdfId = memory.pdf
                assertNotNull(pdfId, "The summary PDF should still be present after patching")
            }
            pdfId!!
        }

        // The PDF must have been regenerated: a new file, with content reflecting the patched data
        assertNotEquals(originalPdfId, newPdfId, "The PDF should have been regenerated (a new file) after patching")
        client.get("/download/$newPdfId").run {
            assertStatusCode(HttpStatusCode.OK)
            assertTrue(!bodyAsBytes().contentEquals(originalPdfBytes), "The regenerated PDF's content should reflect the patched data")
        }

        // The old PDF file is no longer referenced by anything, so it should have been deleted
        client.get("/download/$originalPdfId").assertStatusCode(HttpStatusCode.NotFound)
    }

    @Test
    fun test_delete_memory_notLinkedToLending() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            val user = FakeUser.provideEntity()
            MemoryEntity.new {
                this.text = "Standalone memory"
                this.submittedBy = user
                this.from = ZonedDateTime.fromInstant(Clock.System.now(), TimeZone.currentSystemDefault())
                this.to = ZonedDateTime.fromInstant(Clock.System.now(), TimeZone.currentSystemDefault())
            }
        },
    ) { context ->
        val memory = context.dibResult!!

        // A memory not linked to any lending can always be deleted
        client.delete("/memories/${memory.id.value}").apply {
            assertStatusCode(HttpStatusCode.NoContent)
        }
    }

    @Test
    fun test_delete_memory_forLending_noNewerLending() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            val user = FakeUser.provideEntity()
            val lending = LendingEntity.new {
                this.userSub = user
                this.from = LocalDate.of(2025, 10, 8)
                this.to = LocalDate.of(2025, 10, 9)
                this.returned = true
                this.memorySubmitted = true
                this.memorySubmittedAt = Instant.parse("2025-10-01T10:00:00Z")
                this.timestamp = Instant.parse("2025-09-25T10:00:00Z")
            }
            val memory = MemoryEntity.new {
                this.text = "Everything went great"
                this.submittedBy = user
                this.lending = lending
                this.from = ZonedDateTime.fromInstant(Clock.System.now(), TimeZone.currentSystemDefault())
                this.to = ZonedDateTime.fromInstant(Clock.System.now(), TimeZone.currentSystemDefault())
            }
            Memories.update({ Memories.id eq memory.id }) {
                it[createdAt] = Instant.parse("2025-10-01T10:00:00Z")
            }
            lending to memory
        },
    ) { context ->
        val (lending, memory) = context.dibResult!!

        client.delete("/memories/${memory.id.value}").apply {
            assertStatusCode(HttpStatusCode.NoContent)
        }

        // The lending should be reset back to "memory not submitted", allowing the user to submit a new one
        client.get("/inventory/lendings/${lending.id.value}").apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(Lending.serializer()) { fetchedLending ->
                assertEquals(false, fetchedLending.memorySubmitted)
            }
        }
    }

    @Test
    fun test_delete_memory_forLending_newerLendingExists() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            val user = FakeUser.provideEntity()
            val lending = LendingEntity.new {
                this.userSub = user
                this.from = LocalDate.of(2025, 10, 8)
                this.to = LocalDate.of(2025, 10, 9)
                this.returned = true
                this.memorySubmitted = true
                this.memorySubmittedAt = Instant.parse("2025-10-01T10:00:00Z")
                this.timestamp = Instant.parse("2025-09-25T10:00:00Z")
            }
            val memory = MemoryEntity.new {
                this.text = "Everything went great"
                this.submittedBy = user
                this.lending = lending
                this.from = ZonedDateTime.fromInstant(Clock.System.now(), TimeZone.currentSystemDefault())
                this.to = ZonedDateTime.fromInstant(Clock.System.now(), TimeZone.currentSystemDefault())
            }
            Memories.update({ Memories.id eq memory.id }) {
                it[createdAt] = Instant.parse("2025-10-01T10:00:00Z")
            }

            // The user has already been allowed to create a new lending, relying on this memory being submitted
            LendingEntity.new {
                this.userSub = user
                this.from = LocalDate.of(2025, 11, 10)
                this.to = LocalDate.of(2025, 11, 12)
                this.timestamp = Instant.parse("2025-11-01T10:00:00Z")
            }
            memory
        },
    ) { context ->
        val memory = context.dibResult!!

        // Deleting the memory now would retroactively invalidate the newer lending, so it must be rejected
        client.delete("/memories/${memory.id.value}").apply {
            assertError(Error.CannotDeleteMemoryLendingCreatedAfter())
        }
    }
}
