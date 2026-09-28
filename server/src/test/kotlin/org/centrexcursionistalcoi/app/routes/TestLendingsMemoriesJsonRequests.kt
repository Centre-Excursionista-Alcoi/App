package org.centrexcursionistalcoi.app.routes

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.serialization.KSerializer
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.ResourcesUtils
import org.centrexcursionistalcoi.app.assertBody
import org.centrexcursionistalcoi.app.assertError
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.data.Memory
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.data.ZonedDateTime
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.InventoryItemEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity
import org.centrexcursionistalcoi.app.database.entity.LendingEntity
import org.centrexcursionistalcoi.app.database.entity.LendingUserEntity
import org.centrexcursionistalcoi.app.database.entity.MemoryEntity
import org.centrexcursionistalcoi.app.database.entity.UserInsuranceEntity
import org.centrexcursionistalcoi.app.database.table.LendingItems
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.request.CreateLendingRequest
import org.centrexcursionistalcoi.app.request.CreateMemoryRequest
import org.centrexcursionistalcoi.app.request.PickupLendingRequest
import org.centrexcursionistalcoi.app.request.RequestWithFiles
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.test.FakeUser2
import org.centrexcursionistalcoi.app.test.LoginType
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insert
import kotlinx.datetime.LocalDate as KotlinLocalDate

/**
 * The lending and memory routes receiving JSON. The forms app versions that predate JSON send stay covered by
 * [TestLendingsRoutes] and [TestMemoriesRoutes].
 */
class TestLendingsMemoriesJsonRequests : ApplicationTestBase() {
    private val pdf = ResourcesUtils.bytesFromResource("/document.pdf")
    private val png = ResourcesUtils.bytesFromResource("/image.png")

    private val zone = TimeZone.currentSystemDefault()
    private val from = ZonedDateTime(zone, KotlinLocalDate(2025, 6, 15), LocalTime(10, 0, 0))
    private val to = ZonedDateTime(zone, KotlinLocalDate(2025, 6, 15), LocalTime(12, 0, 0))

    private suspend fun <T> HttpClient.postJson(url: String, serializer: KSerializer<T>, request: T): HttpResponse =
        post(url) {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(serializer, request))
        }

    /** Creates an item, and signs [FakeUser] up for lendings with an insurance for 2025. */
    context(_: JdbcTransaction)
    private fun createItemAndLendingUser(): InventoryItemEntity {
        val type = InventoryItemTypeEntity.new { displayName = "Item Type" }
        val item = InventoryItemEntity.new { this.type = type }
        val user = FakeUser.provideEntity()
        LendingUserEntity.new {
            userSub = user
            phoneNumber = "123456789"
            sports = listOf(Sports.HIKING)
        }
        UserInsuranceEntity.new {
            userSub = user
            validFrom = LocalDate.of(2025, 1, 1)
            validTo = LocalDate.of(2025, 12, 31)
            insuranceCompany = "Insurance Co"
            policyNumber = "POL123456"
        }
        return item
    }

    // ---- POST /inventory/lendings ----

    @Test
    fun test_create_lending() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { createItemAndLendingUser().id.value },
        mockDate = LocalDate.of(2025, 10, 8),
    ) { context ->
        val itemId = context.dibResult!!.toKotlinUuid()
        val location = client.postJson(
            "/inventory/lendings",
            CreateLendingRequest.serializer(),
            CreateLendingRequest(KotlinLocalDate(2025, 10, 10), KotlinLocalDate(2025, 10, 11), listOf(itemId), "Some notes"),
        ).run {
            assertStatusCode(HttpStatusCode.Created)
            assertNotNull(headers[HttpHeaders.Location])
        }

        val lending = Database { LendingEntity[location.substringAfterLast('/').let(Uuid::parse).toJavaUuid()] }
        Database {
            assertEquals(LocalDate.of(2025, 10, 10), lending.from)
            assertEquals(LocalDate.of(2025, 10, 11), lending.to)
            assertEquals("Some notes", lending.notes)
            assertEquals(listOf(itemId), lending.items.map { it.id.value.toKotlinUuid() })
        }
    }

    @Test
    fun test_create_lending_invalid() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { createItemAndLendingUser().id.value },
        mockDate = LocalDate.of(2025, 10, 8),
    ) { context ->
        val itemId = context.dibResult!!.toKotlinUuid()
        client.postJson(
            "/inventory/lendings",
            CreateLendingRequest.serializer(),
            CreateLendingRequest(KotlinLocalDate(2025, 10, 10), KotlinLocalDate(2025, 10, 11), emptyList()),
        ).assertError(Error.ListCannotBeEmpty("items"))
        client.postJson(
            "/inventory/lendings",
            CreateLendingRequest.serializer(),
            CreateLendingRequest(KotlinLocalDate(2025, 10, 11), KotlinLocalDate(2025, 10, 10), listOf(itemId)),
        ).assertError(Error.EndDateCannotBeBeforeStart())
        client.post("/inventory/lendings") {
            contentType(ContentType.Application.Json)
            setBody("""{"from":"2025-10-10","items":["$itemId"]}""")
        }.assertError(Error.MalformedRequest())

        Database { assertTrue(LendingEntity.all().empty()) }
    }

    // ---- POST /inventory/lendings/{id}/pickup ----

    @Test
    fun test_pickup_lending_dismissingItems() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            val type = InventoryItemTypeEntity.new { displayName = "Item Type" }
            val kept = InventoryItemEntity.new { this.type = type }
            val dismissed = InventoryItemEntity.new { this.type = type }
            val lending = LendingEntity.new {
                userSub = FakeUser.provideEntity()
                this.from = LocalDate.of(2025, 10, 10)
                this.to = LocalDate.of(2025, 10, 15)
                confirmed = true
            }
            for (item in listOf(kept, dismissed)) {
                LendingItems.insert {
                    it[LendingItems.item] = item.id
                    it[LendingItems.lending] = lending.id
                }
            }
            Triple(lending.id.value, kept.id.value, dismissed.id.value)
        },
    ) { context ->
        val (lendingId, keptId, dismissedId) = context.dibResult!!
        client.postJson(
            "/inventory/lendings/$lendingId/pickup",
            PickupLendingRequest.serializer(),
            PickupLendingRequest(listOf(dismissedId.toKotlinUuid())),
        ).apply {
            assertStatusCode(HttpStatusCode.NoContent)
            assertEquals(dismissedId.toString(), headers["CEA-Dismissed-Items"])
        }

        Database {
            val lending = LendingEntity[lendingId]
            assertEquals(true, lending.taken)
            assertEquals(listOf(keptId), lending.items.map { it.id.value })
        }
    }

    // ---- POST /memories ----

    @Test
    fun test_create_memory_withAttachments() = runApplicationTest(shouldLogIn = LoginType.USER) {
        val request = CreateMemoryRequest(
            text = "A memory",
            place = "Alcoi",
            sport = Sports.HIKING,
            from = from,
            to = to,
            attachments = listOf(FileWithContext(part = "file_0"), FileWithContext(png, "photo.png", ContentType.Image.PNG)),
        )
        val location = client.post("/memories") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            RequestWithFiles.REQUEST_PART,
                            json.encodeToString(CreateMemoryRequest.serializer(), request),
                            Headers.build { append(HttpHeaders.ContentType, ContentType.Application.Json.toString()) },
                        )
                        append("file_0", pdf, Headers.build { append(HttpHeaders.ContentDisposition, "filename=report.pdf") })
                    }
                )
            )
        }.run {
            assertStatusCode(HttpStatusCode.Created)
            assertNotNull(headers[HttpHeaders.Location])
        }

        var memory: Memory? = null
        client.get(location).assertBody(Memory.serializer()) { memory = it }
        val attachments = memory!!.attachments
        assertEquals("A memory", memory.text)
        assertEquals("Alcoi", memory.place)
        assertEquals(Sports.HIKING, memory.sport)
        assertEquals(from, memory.from)
        assertEquals(to, memory.to)
        assertEquals(2, attachments.size)
        assertEquals(
            setOf(pdf.toList(), png.toList()),
            attachments.map { client.get("/download/$it").bodyAsBytes().toList() }.toSet(),
        )

        // Only the submitter can download them
        Database { FakeUser2.provideEntity() }
        loginAsFakeUser2()
        for (attachment in attachments) {
            client.get("/download/$attachment").assertStatusCode(HttpStatusCode.Forbidden)
        }
    }

    @Test
    fun test_create_memory_invalid() = runApplicationTest(shouldLogIn = LoginType.USER) {
        client.postJson("/memories", CreateMemoryRequest.serializer(), CreateMemoryRequest(text = " ", from = from, to = to))
            .assertError(Error.MemoryNotGiven())
        client.postJson("/memories", CreateMemoryRequest.serializer(), CreateMemoryRequest(text = "A memory", to = to))
            .assertError(Error.MissingArgument("from"))
        client.postJson(
            "/memories",
            CreateMemoryRequest.serializer(),
            CreateMemoryRequest(text = "A memory", from = from, to = to, attachments = listOf(FileWithContext(part = "file_0"))),
        ).assertError(Error.MalformedRequest())

        Database { assertTrue(MemoryEntity.all().empty()) }
    }

    @Test
    fun test_create_memory_forLending() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            LendingEntity.new {
                userSub = FakeUser.provideEntity()
                this.from = LocalDate.of(2025, 10, 8)
                this.to = LocalDate.of(2025, 10, 9)
                returned = true
            }.id.value
        },
    ) { context ->
        val lendingId = context.dibResult!!
        val location = client.postJson(
            "/memories",
            CreateMemoryRequest.serializer(),
            CreateMemoryRequest(text = "Everything went great", lending = lendingId.toKotlinUuid()),
        ).run {
            assertStatusCode(HttpStatusCode.Created)
            assertNotNull(headers[HttpHeaders.Location])
        }

        client.get(location).assertBody(Memory.serializer()) { memory ->
            assertEquals(lendingId.toKotlinUuid(), memory.lending)
            assertEquals(ZonedDateTime(zone, LocalDate.of(2025, 10, 8).toKotlinLocalDate(), LocalTime(0, 0, 0)), memory.from)
        }
        Database { assertEquals(true, LendingEntity[lendingId].memorySubmitted) }
    }

    @Test
    fun test_create_memory_legacyForm_withAttachment() = runApplicationTest(shouldLogIn = LoginType.USER) {
        val location = client.submitFormWithBinaryData(
            "/memories",
            formData {
                append("text", "A memory")
                append("from", from.toString())
                append("to", to.toString())
                append("file_0", pdf, Headers.build { append(HttpHeaders.ContentDisposition, "filename=report.pdf") })
            }
        ).run {
            assertStatusCode(HttpStatusCode.Created)
            assertNotNull(headers[HttpHeaders.Location])
        }

        var memory: Memory? = null
        client.get(location).assertBody(Memory.serializer()) { memory = it }
        val attachment = memory!!.attachments.single()
        assertContentEquals(pdf, client.get("/download/$attachment").bodyAsBytes())
    }
}
