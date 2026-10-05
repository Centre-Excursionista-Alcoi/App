package org.centrexcursionistalcoi.app.routes

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.statement.readRawBytes
import io.ktor.http.contentType
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.data.Category
import org.centrexcursionistalcoi.app.data.CategoryPrice
import org.centrexcursionistalcoi.app.data.PriceUnit
import org.centrexcursionistalcoi.app.database.entity.SpaceEntity
import org.centrexcursionistalcoi.app.database.entity.SpaceKeyEntity
import org.centrexcursionistalcoi.app.database.entity.SpaceKeyTypeEntity
import org.centrexcursionistalcoi.app.database.table.SpaceKeyTypeSpaces
import org.jetbrains.exposed.v1.jdbc.insert
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.test.FakeAdminUser
import org.centrexcursionistalcoi.app.notifications.Email
import org.centrexcursionistalcoi.app.database.UserPreferenceKey
import org.centrexcursionistalcoi.app.database.UserPreferenceStore
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.centrexcursionistalcoi.app.assertError
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.data.Space
import org.centrexcursionistalcoi.app.data.SpaceKeyType
import org.centrexcursionistalcoi.app.data.SpaceLending
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.data.PaymentStatus
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.database.entity.SpaceLendingEntity
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.database.table.SpaceLendings
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.request.SubmitSpaceLendingReportRequest
import org.centrexcursionistalcoi.app.utils.requestWithFilesBody
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.int
import kotlin.test.assertContentEquals
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.jdbc.update
import org.centrexcursionistalcoi.app.test.FakeUser2
import org.centrexcursionistalcoi.app.SPACES_MANAGER_GROUP_NAME
import org.centrexcursionistalcoi.app.SPACE_LENDINGS_MANAGER_GROUP_NAME
import org.centrexcursionistalcoi.app.test.LoginType
import org.centrexcursionistalcoi.app.utils.toUuid
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

class TestSpaceLendingsRoutes : ApplicationTestBase() {
    private val spaceId = "5c1c3f0e-0b0a-4c3a-9d55-0a1b2c3d4e5f".toUuid()
    private val carKeyId = "6c1c3f0e-0b0a-4c3a-9d55-0a1b2c3d4e5f".toUuid()

    context(_: JdbcTransaction)
    private fun createSpace(conditions: String? = null, closed: Boolean = false) {
        SpaceEntity.new(spaceId) {
            name = "Casa"
            description = "A house"
            conditionsOfUse = conditions
            requiresKeys = true
            isClosed = closed
            prices = listOf(
                CategoryPrice(Category.MEMBER, 3.0, PriceUnit.PER_NIGHT),
                CategoryPrice(Category.NON_MEMBER, 6.0, PriceUnit.PER_NIGHT),
            )
        }
        val carType = SpaceKeyTypeEntity.new(carKeyId) { name = "Car" }
        SpaceKeyTypeSpaces.insert {
            it[keyType] = carType.id
            it[space] = spaceId
            it[maxPerLending] = 3
        }
        // The club has 3 car permits
        repeat(3) { SpaceKeyEntity.new { this.type = carType; label = "${it + 1}" } }
    }

    private fun body(
        checkIn: String,
        checkOut: String,
        attendees: String = """{"MEMBER":2}""",
        extra: String = "",
    ) = """{"space":"$spaceId","checkIn":"$checkIn","checkOut":"$checkOut","attendees":$attendees$extra}"""

    private suspend fun HttpClient.book(json: String): HttpResponse = post("/space_lendings") {
        contentType(ContentType.Application.Json)
        setBody(json)
    }

    private val today = LocalDate(2026, 10, 1)

    /** Marks every lending as paid, so the user can make another one. */
    private fun payAll() = Database { SpaceLendings.update({ SpaceLendings.id.isNotNull() }) { it[paymentStatus] = PaymentStatus.COMPLETED } }

    private suspend fun HttpClient.postJson(url: String, json: String = "{}"): HttpResponse = post(url) {
        contentType(ContentType.Application.Json)
        setBody(json)
    }

    private fun lendingId(location: String) = location.substringAfterLast('/')

    /** Makes the logged in user a space lendings manager. */
    private fun becomeManager(sub: String) = Database {
        val ref = UserReferenceEntity[sub]
        ref.groups = ref.groups + SPACE_LENDINGS_MANAGER_GROUP_NAME
    }

    @Test
    fun test_notLoggedIn() = runApplicationTest { client.post("/space_lendings").assertStatusCode(HttpStatusCode.Unauthorized) }

    @Test
    fun test_adjacent_stays_do_not_collide_but_overlapping_do() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { createSpace() },
        mockDate = today,
    ) {
        client.book(body("2026-10-09", "2026-10-10")).assertStatusCode(HttpStatusCode.Created)
        payAll()
        client.book(body("2026-10-10", "2026-10-11")).assertStatusCode(HttpStatusCode.Created)
        payAll()
        client.book(body("2026-10-10", "2026-10-12")).assertStatusCode(HttpStatusCode.Conflict)
        client.book(body("2026-10-08", "2026-10-09")).assertStatusCode(HttpStatusCode.Created)
    }

    @Test
    fun test_price_is_computed_and_attendees_can_change() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { createSpace() },
        mockDate = today,
    ) {
        val response = client.book(body("2026-10-09", "2026-10-11", """{"MEMBER":2,"NON_MEMBER":1}"""))
        response.assertStatusCode(HttpStatusCode.Created)
        val location = response.headers["Location"]!!

        fun JsonObject.price() = getValue("totalPrice").jsonPrimitive.double
        suspend fun fetch() = Json.parseToJsonElement(client.get(location).bodyAsText()).jsonObject

        assertEquals((2 * 3.0 + 6.0) * 2, fetch().price())

        client.post("$location/attendees") {
            contentType(ContentType.Application.Json)
            setBody("""{"attendees":{"MEMBER":1}}""")
        }.assertStatusCode(HttpStatusCode.NoContent)
        assertEquals(3.0 * 2, fetch().price())
        assertEquals("PENDING", fetch().getValue("paymentStatus").jsonPrimitive.content)
    }

    @Test
    fun test_cancelled_lending_frees_dates() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            createSpace()
            FakeUser2.provideEntity()
        },
        mockDate = today,
    ) {
        val location = client.book(body("2026-10-09", "2026-10-11")).also { it.assertStatusCode(HttpStatusCode.Created) }.headers["Location"]!!
        loginAs(FakeUser2)
        client.book(body("2026-10-09", "2026-10-10")).assertStatusCode(HttpStatusCode.Conflict)
        loginAs(FakeUser)
        client.post("$location/cancel").assertStatusCode(HttpStatusCode.NoContent)
        loginAs(FakeUser2)
        client.book(body("2026-10-09", "2026-10-10")).assertStatusCode(HttpStatusCode.Created)
    }

    @Test
    fun test_validation() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { createSpace(conditions = "Be nice") },
        mockDate = today,
    ) {
        // Conditions must be accepted
        client.book(body("2026-10-09", "2026-10-10")).assertStatusCode(HttpStatusCode.BadRequest)
        val accept = ""","acceptConditions":true"""
        client.book(body("2026-10-10", "2026-10-09", extra = accept)).assertStatusCode(HttpStatusCode.BadRequest)
        client.book(body("2026-09-01", "2026-09-02", extra = accept)).assertStatusCode(HttpStatusCode.BadRequest)
        client.book(body("2026-10-09", "2026-10-10", attendees = """{"MEMBER":0}""", extra = accept)).assertStatusCode(HttpStatusCode.BadRequest)
        // Keys: no more than the maximum
        client.book(body("2026-10-09", "2026-10-10", extra = """$accept,"keys":{"$carKeyId":4}""")).assertStatusCode(HttpStatusCode.BadRequest)
        client.book(body("2026-10-09", "2026-10-10", extra = """$accept,"keys":{"$carKeyId":3}""")).assertStatusCode(HttpStatusCode.Created)
    }

    @Test
    fun test_closed_space_cannot_be_booked() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { createSpace(closed = true) },
        mockDate = today,
    ) {
        client.book(body("2026-10-09", "2026-10-10")).assertStatusCode(HttpStatusCode.Conflict)
    }

    @Test
    fun test_other_users_cannot_see_a_lending() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            createSpace()
            FakeUser2.provideEntity()
        },
        mockDate = today,
    ) { context ->
        val location = client.book(body("2026-10-09", "2026-10-10")).headers["Location"]
        assertNotNull(location)
        client.get(location).assertStatusCode(HttpStatusCode.OK)
        loginAs(FakeUser2)
        client.get(location).assertStatusCode(HttpStatusCode.NotFound)
        client.post("$location/cancel").assertStatusCode(HttpStatusCode.NotFound)
        // ... but can see which nights are taken
        client.get("/spaces/$spaceId/occupancy").apply {
            assertStatusCode(HttpStatusCode.OK)
            assertEquals(1, Json.parseToJsonElement(bodyAsText()).let { (it as kotlinx.serialization.json.JsonArray).size })
        }
        // ... and the list is empty
        assertEquals("[]", client.get("/space_lendings").bodyAsText())
    }

    @Test
    fun test_only_admins_hand_out_keys() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { createSpace() },
        mockDate = today,
    ) {
        val location = client.book(body("2026-10-09", "2026-10-10", extra = ""","keys":{"$carKeyId":2}""")).headers["Location"]!!
        client.post("$location/pickup").assertStatusCode(HttpStatusCode.NotFound)
        client.post("$location/return").assertStatusCode(HttpStatusCode.NotFound)
    }

    @Test
    fun test_space_lendings_manager_sees_all_and_sets_payment() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            createSpace()
            FakeUser2.provideEntity()
        },
        userEntityPatches = { it.groups = it.groups + SPACE_LENDINGS_MANAGER_GROUP_NAME },
        mockDate = today,
    ) {
        // The logged in user is a manager, but books as themselves
        val location = client.book(body("2026-10-09", "2026-10-10")).headers["Location"]!!
        // It cannot be paid before the keys are returned
        client.postJson("$location/payment", """{"status":"COMPLETED"}""").assertStatusCode(HttpStatusCode.Conflict)
        client.post("$location/pickup").assertStatusCode(HttpStatusCode.NoContent)
        client.post("$location/return").assertStatusCode(HttpStatusCode.NoContent)
        client.post("$location/payment") {
            contentType(ContentType.Application.Json)
            setBody("""{"status":"COMPLETED"}""")
        }.assertStatusCode(HttpStatusCode.NoContent)
        assertEquals("COMPLETED", Json.parseToJsonElement(client.get(location).bodyAsText()).jsonObject.getValue("paymentStatus").jsonPrimitive.content)
    }

    @Test
    fun test_manager_can_see_other_users_lendings() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            createSpace()
            FakeUser2.provideEntity()
        },
        mockDate = today,
    ) {
        val location = client.book(body("2026-10-09", "2026-10-10")).headers["Location"]!!
        // User 2 becomes a manager
        loginAs(FakeUser2)
        client.get(location).assertStatusCode(HttpStatusCode.NotFound)
        org.centrexcursionistalcoi.app.database.Database {
            val ref = org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity[FakeUser2.SUB]
            ref.groups = ref.groups + SPACE_LENDINGS_MANAGER_GROUP_NAME
        }
        client.get(location).assertStatusCode(HttpStatusCode.OK)
        assertEquals(1, (Json.parseToJsonElement(client.get("/space_lendings").bodyAsText()) as kotlinx.serialization.json.JsonArray).size)
    }

    @Test
    fun test_spaces_manager_can_create_spaces_but_regular_user_cannot() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { FakeUser2.provideEntity() },
        mockDate = today,
    ) {
        val create: suspend () -> HttpResponse = {
            client.post("/spaces") {
                contentType(ContentType.Application.Json)
                setBody("""{"name":"Casa","description":"A house"}""")
            }
        }
        create().assertStatusCode(HttpStatusCode.Forbidden)
        org.centrexcursionistalcoi.app.database.Database {
            val ref = org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity[FakeUser.SUB]
            ref.groups = ref.groups + SPACES_MANAGER_GROUP_NAME
        }
        // Group changes apply immediately: sessions re-read the user on each request
        create().assertStatusCode(HttpStatusCode.Created)
        // A space lendings manager is not a spaces manager
        org.centrexcursionistalcoi.app.database.Database {
            val ref = org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity[FakeUser.SUB]
            ref.groups = listOf(SPACE_LENDINGS_MANAGER_GROUP_NAME)
        }
        create().assertStatusCode(HttpStatusCode.Forbidden)
    }

    @Test
    fun test_previous_lending_must_be_paid_before_a_new_one() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { createSpace() },
        mockDate = today,
    ) {
        val first = client.book(body("2026-10-09", "2026-10-10")).headers["Location"]!!
        client.book(body("2026-10-20", "2026-10-21")).assertError(Error.PreviousSpaceLendingNotPaid())
        // A cancelled one doesn't count
        client.post("$first/cancel").assertStatusCode(HttpStatusCode.NoContent)
        val second = client.book(body("2026-10-20", "2026-10-21")).also { it.assertStatusCode(HttpStatusCode.Created) }.headers["Location"]!!
        client.book(body("2026-11-20", "2026-11-21")).assertError(Error.PreviousSpaceLendingNotPaid())
        // Neither does a paid one
        becomeManager(FakeUser.SUB)
        client.post("$second/pickup").assertStatusCode(HttpStatusCode.NoContent)
        client.post("$second/return").assertStatusCode(HttpStatusCode.NoContent)
        client.book(body("2026-11-20", "2026-11-21")).assertError(Error.PreviousSpaceLendingNotPaid())
        client.postJson("$second/payment", """{"status":"COMPLETED"}""").assertStatusCode(HttpStatusCode.NoContent)
        client.book(body("2026-11-20", "2026-11-21")).assertStatusCode(HttpStatusCode.Created)
    }

    @Test
    fun test_pipeline_lending_can_be_modified_until_pickup() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            createSpace()
            FakeUser2.provideEntity()
        },
        mockDate = today,
    ) {
        val location = client.book(body("2026-10-09", "2026-10-10")).headers["Location"]!!
        suspend fun fetch() = Json.parseToJsonElement(client.get(location).bodyAsText()).jsonObject

        // Change dates, attendees and keys
        client.patch(location) {
            contentType(ContentType.Application.Json)
            setBody("""{"checkIn":"2026-10-14","checkOut":"2026-10-16","attendees":{"MEMBER":3},"keys":{"$carKeyId":2}}""")
        }.assertStatusCode(HttpStatusCode.NoContent)
        fetch().apply {
            assertEquals("2026-10-14", getValue("checkIn").jsonPrimitive.content)
            assertEquals(3 * 3.0 * 2, getValue("totalPrice").jsonPrimitive.double)
            assertEquals(2, getValue("requestedKeys").jsonObject.getValue("$carKeyId").jsonPrimitive.int)
        }
        // Following availability: someone else's lending blocks it
        loginAs(FakeUser2)
        client.book(body("2026-10-20", "2026-10-22")).assertStatusCode(HttpStatusCode.Created)
        loginAs(FakeUser)
        client.patch(location) {
            contentType(ContentType.Application.Json)
            setBody("""{"checkIn":"2026-10-21","checkOut":"2026-10-23"}""")
        }.assertStatusCode(HttpStatusCode.Conflict)
        // ... but its own dates don't
        client.patch(location) {
            contentType(ContentType.Application.Json)
            setBody("""{"checkOut":"2026-10-17"}""")
        }.assertStatusCode(HttpStatusCode.NoContent)

        // Once the keys are picked up, it is locked
        becomeManager(FakeUser.SUB)
        client.post("$location/pickup").assertStatusCode(HttpStatusCode.NoContent)
        assertNotNull(fetch()["pickedUpAt"])
        client.patch(location) {
            contentType(ContentType.Application.Json)
            setBody("""{"checkOut":"2026-10-18"}""")
        }.assertStatusCode(HttpStatusCode.Conflict)
        client.post("$location/cancel").assertStatusCode(HttpStatusCode.Conflict)
        client.post("$location/pickup").assertStatusCode(HttpStatusCode.Conflict)
        // Attendees can still change until paid
        client.postJson("$location/attendees", """{"attendees":{"MEMBER":1}}""").assertStatusCode(HttpStatusCode.NoContent)
    }

    @Test
    fun test_free_lending_is_paid_when_returned() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { createSpace() },
        mockDate = today,
    ) {
        becomeManager(FakeUser.SUB)
        // Day use is free, the space charges nights
        val location = client.book(body("2026-10-09", "2026-10-09")).headers["Location"]!!
        client.post("$location/return").assertStatusCode(HttpStatusCode.Conflict)
        client.post("$location/pickup").assertStatusCode(HttpStatusCode.NoContent)
        client.post("$location/return").assertStatusCode(HttpStatusCode.NoContent)
        assertEquals("COMPLETED", Json.parseToJsonElement(client.get(location).bodyAsText()).jsonObject.getValue("paymentStatus").jsonPrimitive.content)
    }

    private fun jpegWithExif() = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE1.toByte(), 0x00, 0x16) +
        "Exif".toByteArray() + byteArrayOf(0, 0) + "GPS+Camera+Date".toByteArray() + byteArrayOf(0xFF.toByte(), 0xD9.toByte())

    @Test
    fun test_report_only_when_over_and_photos_are_kept_untouched() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            createSpace()
            val user = FakeUser.provideEntity()
            fun lending(checkIn: LocalDate, checkOut: LocalDate) = SpaceLendingEntity.new {
                userSub = user
                space = SpaceEntity[spaceId]
                this.checkIn = checkIn
                this.checkOut = checkOut
                attendees = mapOf(Category.MEMBER to 1)
            }.id.value
            lending(LocalDate(2026, 9, 20), LocalDate(2026, 9, 21)) to lending(LocalDate(2026, 10, 20), LocalDate(2026, 10, 21))
        },
        mockDate = today,
    ) { context ->
        val (over, upcoming) = context.dibResult!!
        val photo = jpegWithExif()
        val request = SubmitSpaceLendingReportRequest(
            reportNotes = "Everything was left as found",
            reportIssues = "The tap leaks",
            reportNotesFiles = listOf(FileWithContext(photo, name = "IMG_0001.jpg", contentType = ContentType.Image.JPEG)),
            reportIssuesFiles = listOf(FileWithContext(photo, name = "IMG_0002.jpg", contentType = ContentType.Image.JPEG)),
        )
        suspend fun report(id: Uuid) = client.post("/space_lendings/$id/report") {
            setBody(requestWithFilesBody(request, SubmitSpaceLendingReportRequest.serializer()))
        }

        // Not over yet
        report(upcoming).assertStatusCode(HttpStatusCode.Conflict)

        report(over).assertStatusCode(HttpStatusCode.NoContent)
        val lending = Json.parseToJsonElement(client.get("/space_lendings/$over").bodyAsText()).jsonObject
        assertEquals("Everything was left as found", lending.getValue("reportNotes").jsonPrimitive.content)
        assertEquals("The tap leaks", lending.getValue("reportIssues").jsonPrimitive.content)

        // The photos come back byte by byte as they were sent, so all their metadata is kept
        for ((field, name) in listOf("reportNotesFiles" to "IMG_0001.jpg", "reportIssuesFiles" to "IMG_0002.jpg")) {
            val fileId = lending.getValue(field).jsonArray.single().jsonPrimitive.content
            val download = client.get("/download/$fileId")
            download.assertStatusCode(HttpStatusCode.OK)
            assertContentEquals(photo, download.readRawBytes())
            assertEquals(ContentType.Image.JPEG, download.contentType()?.withoutParameters())
            assertEquals(name, Database { FileEntity[fileId.toUuid()].name })
        }
    }

    @Test
    fun test_responses_can_be_decoded_by_the_app() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { createSpace() },
        mockDate = today,
    ) {
        // The app decodes what the server answers into the shared data classes
        val lendingLocation = client.book(body("2026-10-09", "2026-10-10", extra = ""","keys":{"$carKeyId":2}""")).headers["Location"]!!

        val spaces = json.decodeFromString(ListSerializer(Space.serializer()), client.get("/spaces").bodyAsText())
        assertEquals("Casa", spaces.single().name)
        assertEquals(2, spaces.single().prices.size)

        val types = json.decodeFromString(ListSerializer(SpaceKeyType.serializer()), client.get("/space_key_types").bodyAsText())
        assertEquals(3, types.single().spaces.single().maxPerLending)

        val lending = json.decodeFromString(SpaceLending.serializer(), client.get(lendingLocation).bodyAsText())
        assertEquals(mapOf(Category.MEMBER to 2), lending.attendees)
        assertEquals(mapOf(carKeyId to 2), lending.requestedKeys)
        val all = json.decodeFromString(ListSerializer(SpaceLending.serializer()), client.get("/space_lendings").bodyAsText())
        assertEquals(lending.id, all.single().id)
    }

    @Test
    fun test_new_lending_emails_the_user_and_the_managers_in_their_language() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            createSpace()
            // A manager, who gets the notice
            FakeAdminUser.provideEntity()
        },
        mockDate = today,
        disableEmail = false,
    ) {
        val sent = mutableListOf<Email.SentEmail>()
        Email.sent = sent
        try {
            client.post("/space_lendings") {
                header(HttpHeaders.AcceptLanguage, "ca")
                contentType(ContentType.Application.Json)
                setBody(body("2026-10-09", "2026-10-10"))
            }.assertStatusCode(HttpStatusCode.Created)

            // They are sent in the background
            withContext(Dispatchers.Default) { withTimeout(10_000) { while (sent.size < 2) delay(50) } }

            val toUser = sent.single { it.to.any { to -> to.email == FakeUser.EMAIL } }
            assertEquals("El teu lloguer d'espai està confirmat", toUser.subject)
            assertTrue("Casa" in toUser.htmlContent, toUser.htmlContent)
            assertTrue("/space_lendings/" in toUser.htmlContent, toUser.htmlContent)

            val toStaff = sent.single { it !== toUser }
            assertTrue(toStaff.to.any { it.email == FakeAdminUser.EMAIL })
            assertTrue(toStaff.subject.startsWith("New space lending (#"), toStaff.subject)
            assertTrue("/admin/space_lendings/" in toStaff.htmlContent, toStaff.htmlContent)
        } finally {
            Email.sent = null
        }
    }

    @Test
    fun test_staff_emails_are_in_the_language_of_each_recipient() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            createSpace()
            val admin = FakeAdminUser.provideEntity()
            UserPreferenceStore[admin.id.value, UserPreferenceKey.Language] = java.util.Locale.forLanguageTag("es")
        },
        mockDate = today,
        disableEmail = false,
    ) {
        val sent = mutableListOf<Email.SentEmail>()
        Email.sent = sent
        try {
            client.post("/space_lendings") {
                contentType(ContentType.Application.Json)
                setBody(body("2026-10-09", "2026-10-10"))
            }.assertStatusCode(HttpStatusCode.Created)
            withContext(Dispatchers.Default) { withTimeout(10_000) { while (sent.size < 2) delay(50) } }

            val toStaff = sent.single { it.to.any { to -> to.email == FakeAdminUser.EMAIL } }
            assertTrue(toStaff.subject.startsWith("Nuevo alquiler de espacio (#"), toStaff.subject)
            // The user has no language: english
            val toUser = sent.single { it !== toStaff }
            assertEquals("Your space lending is confirmed", toUser.subject)
        } finally {
            Email.sent = null
        }
    }

    private fun carKeys() = Database { SpaceKeyEntity.all().map { it.id.value.toString() }.sorted() }

    @Test
    fun test_keys_are_limited_by_the_club_inventory() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            createSpace()
            FakeUser2.provideEntity()
        },
        mockDate = today,
    ) {
        // 2 of the 3 permits
        client.book(body("2026-10-09", "2026-10-11", extra = ""","keys":{"$carKeyId":2}""")).assertStatusCode(HttpStatusCode.Created)
        payAll()
        // Only 1 is left for those nights
        client.book(body("2026-10-10", "2026-10-12", extra = ""","keys":{"$carKeyId":2}""")).apply {
            assertStatusCode(HttpStatusCode.Conflict)
        }
    }

    @Test
    fun test_keys_are_free_again_for_other_nights() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { createSpace() },
        mockDate = today,
    ) {
        client.book(body("2026-10-09", "2026-10-10", extra = ""","keys":{"$carKeyId":3}""")).assertStatusCode(HttpStatusCode.Created)
        payAll()
        client.book(body("2026-10-10", "2026-10-11", extra = ""","keys":{"$carKeyId":3}""")).assertStatusCode(HttpStatusCode.Created)
    }

    @Test
    fun test_a_key_type_is_shared_by_several_spaces() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            createSpace()
            val otherSpace = SpaceEntity.new { name = "Refugi"; description = "A shelter"; prices = emptyList() }
            SpaceKeyTypeSpaces.insert {
                it[keyType] = carKeyId
                it[space] = otherSpace.id
                it[maxPerLending] = 3
            }
        },
        mockDate = today,
    ) {
        val otherSpace = Database { SpaceEntity.all().first { it.name == "Refugi" }.id.value }
        client.book(body("2026-10-09", "2026-10-10", extra = ""","keys":{"$carKeyId":2}""")).assertStatusCode(HttpStatusCode.Created)
        payAll()
        // The same permits are needed in the other space on the same nights
        client.book(
            """{"space":"$otherSpace","checkIn":"2026-10-09","checkOut":"2026-10-10","attendees":{"MEMBER":1},"keys":{"$carKeyId":2}}"""
        ).assertStatusCode(HttpStatusCode.Conflict)
    }

    @Test
    fun test_keys_from_a_type_not_for_the_space_are_rejected() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            createSpace()
            SpaceKeyTypeEntity.new { name = "Other" }
        },
        mockDate = today,
    ) {
        val other = Database { SpaceKeyTypeEntity.all().first { it.name == "Other" }.id.value }
        client.book(body("2026-10-09", "2026-10-10", extra = ""","keys":{"$other":1}""")).assertStatusCode(HttpStatusCode.BadRequest)
    }

    @Test
    fun test_pickup_assigns_keys_and_they_are_returned_one_by_one() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { createSpace() },
        userEntityPatches = { it.groups = it.groups + SPACE_LENDINGS_MANAGER_GROUP_NAME },
        mockDate = today,
    ) {
        val location = client.book(body("2026-10-09", "2026-10-10", extra = ""","keys":{"$carKeyId":2}""")).headers["Location"]!!
        val (a, b, c) = carKeys()

        // More keys than asked for, a key twice, or one that doesn't exist
        client.postJson("$location/pickup", """{"keys":["$a","$b","$c"]}""").assertStatusCode(HttpStatusCode.BadRequest)
        client.postJson("$location/pickup", """{"keys":["$a","$a"]}""").assertStatusCode(HttpStatusCode.BadRequest)
        client.postJson("$location/pickup", """{"keys":["7c1c3f0e-0b0a-4c3a-9d55-0a1b2c3d4e5f"]}""").assertStatusCode(HttpStatusCode.BadRequest)

        client.postJson("$location/pickup", """{"keys":["$a","$b"]}""").assertStatusCode(HttpStatusCode.NoContent)
        val given = Json.parseToJsonElement(client.get(location).bodyAsText()).jsonObject.getValue("keys").jsonArray
        assertEquals(setOf(a, b), given.map { it.jsonObject.getValue("key").jsonPrimitive.content }.toSet())

        // Only one comes back: the lending is not over
        client.postJson("$location/return", """{"keys":["$a"]}""").assertStatusCode(HttpStatusCode.NoContent)
        assertEquals(null, Database { SpaceLendingEntity.all().single().returnedAt })
        // It cannot come back twice, nor can a key that wasn't given
        client.postJson("$location/return", """{"keys":["$a"]}""").assertStatusCode(HttpStatusCode.BadRequest)
        client.postJson("$location/return", """{"keys":["$c"]}""").assertStatusCode(HttpStatusCode.BadRequest)

        client.postJson("$location/return", """{"keys":["$b"]}""").assertStatusCode(HttpStatusCode.NoContent)
        assertEquals(true, Database { SpaceLendingEntity.all().single().returnedAt != null })
    }

    @Test
    fun test_a_key_out_cannot_be_given_again() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { createSpace() },
        userEntityPatches = { it.groups = it.groups + SPACE_LENDINGS_MANAGER_GROUP_NAME },
        mockDate = today,
    ) {
        val first = client.book(body("2026-10-09", "2026-10-10", extra = ""","keys":{"$carKeyId":1}""")).headers["Location"]!!
        payAll()
        val second = client.book(body("2026-10-20", "2026-10-21", extra = ""","keys":{"$carKeyId":1}""")).headers["Location"]!!
        val (a) = carKeys()
        client.postJson("$first/pickup", """{"keys":["$a"]}""").assertStatusCode(HttpStatusCode.NoContent)
        client.postJson("$second/pickup", """{"keys":["$a"]}""").assertStatusCode(HttpStatusCode.BadRequest)
        // Without a list, everything outstanding comes back
        client.post("$first/return").assertStatusCode(HttpStatusCode.NoContent)
        client.postJson("$second/pickup", """{"keys":["$a"]}""").assertStatusCode(HttpStatusCode.NoContent)
    }
}
