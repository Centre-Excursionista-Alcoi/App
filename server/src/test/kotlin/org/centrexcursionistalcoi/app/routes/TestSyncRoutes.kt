package org.centrexcursionistalcoi.app.routes

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.HttpHeaders
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.data.Category
import org.centrexcursionistalcoi.app.data.CategoryPrice
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.SpaceEntity
import org.centrexcursionistalcoi.app.database.entity.SpaceKeyEntity
import org.centrexcursionistalcoi.app.database.entity.SpaceLendingEntity
import org.centrexcursionistalcoi.app.database.entity.UserReferenceEntity
import org.centrexcursionistalcoi.app.test.FakeAdminUser
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.test.LoginType

class TestSyncRoutes : ApplicationTestBase() {
    /** The route of each section's own request. */
    private val routes = mapOf(
        "profile" to "/profile",
        "departments" to "/departments",
        "users" to "/users",
        "members" to "/members",
        "posts" to "/posts",
        "events" to "/events",
        "inventory_types" to "/inventory/types",
        "inventory_items" to "/inventory/items",
        "lendings" to "/inventory/lendings",
        "memories" to "/memories",
        "spaces" to "/spaces",
        "space_keys" to "/space_keys",
        "space_lendings" to "/space_lendings",
    )

    /** Lists come in no particular order. */
    private fun JsonElement.normalized(): Any = if (this is JsonArray) map { it.normalized() }.toSet() else this

    private fun seed(user: UserReferenceEntity) {
        DepartmentEntity.new { displayName = "Department" }
        val space = SpaceEntity.new {
            name = "Casa"
            description = "A house"
            prices = listOf(CategoryPrice(Category.MEMBER, 3.0))
        }
        SpaceKeyEntity.new {
            this.space = space
            name = "Door"
            maxQuantity = 1
        }
        SpaceLendingEntity.new {
            userSub = user
            this.space = space
            checkIn = LocalDate(2026, 10, 9)
            checkOut = LocalDate(2026, 10, 10)
            attendees = mapOf(Category.MEMBER to 1)
        }
    }

    @Test
    fun test_notLoggedIn() = runApplicationTest { client.get("/sync").assertStatusCode(HttpStatusCode.Unauthorized) }

    @Test
    fun test_sections_are_what_their_own_routes_answer_for_an_admin() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = { seed(FakeAdminUser.provideEntity()) },
    ) {
        assertSyncMatchesRoutes()
    }

    @Test
    fun test_sections_are_what_their_own_routes_answer_for_a_regular_user() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { seed(FakeUser.provideEntity()) },
    ) {
        assertSyncMatchesRoutes()
    }

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.assertSyncMatchesRoutes() {
        val response = client.get("/sync")
        response.assertStatusCode(HttpStatusCode.OK)
        val sync = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertNotNull(sync["serverTime"])

        for ((key, route) in routes) {
            val section = sync[key]?.jsonObject
            assertNotNull(section, "Missing section $key")
            assertTrue(section.getValue("modified").jsonPrimitive.boolean, "Section $key not sent")
            val own = client.get(route)
            own.assertStatusCode(HttpStatusCode.OK)
            assertEquals(
                Json.parseToJsonElement(own.bodyAsText()).normalized(),
                section.getValue("items").normalized(),
                "Section $key differs from $route",
            )
        }
    }

    @Test
    fun test_sections_not_modified_since_the_given_time_are_not_sent() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = { seed(FakeAdminUser.provideEntity()) },
    ) {
        val future = System.currentTimeMillis() + 60_000
        val sync = Json.parseToJsonElement(
            client.get("/sync?spaces=$future&space_keys=0&profile=$future").bodyAsText()
        ).jsonObject

        // Nothing newer than the given time
        assertEquals(false, sync.getValue("spaces").jsonObject.getValue("modified").jsonPrimitive.boolean)
        assertEquals(false, sync.getValue("profile").jsonObject.getValue("modified").jsonPrimitive.boolean)
        assertEquals(null, sync.getValue("spaces").jsonObject["items"])
        // ... but changed since 1970, and the others have no time given
        assertEquals(true, sync.getValue("space_keys").jsonObject.getValue("modified").jsonPrimitive.boolean)
        assertEquals(true, sync.getValue("space_lendings").jsonObject.getValue("modified").jsonPrimitive.boolean)
    }

    @Test
    fun test_only_the_sync_is_compressed() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = { seed(FakeAdminUser.provideEntity()) },
    ) {
        val plain = client.get("/sync").bodyAsText()

        val compressed = client.get("/sync") { header(HttpHeaders.AcceptEncoding, "gzip") }
        assertEquals("gzip", compressed.headers[HttpHeaders.ContentEncoding])
        // Once uncompressed, it's the same
        val bytes = compressed.readRawBytes()
        assertTrue(bytes.size < plain.length, "The compressed response should be smaller")
        val uncompressed = GZIPInputStream(ByteArrayInputStream(bytes)).readBytes().decodeToString()
        assertEquals(
            Json.parseToJsonElement(plain).jsonObject.filterKeys { it != "serverTime" },
            Json.parseToJsonElement(uncompressed).jsonObject.filterKeys { it != "serverTime" },
        )

        // Without asking for it, it is not
        assertEquals(null, client.get("/sync").headers[HttpHeaders.ContentEncoding])
        // Other routes are never compressed
        assertEquals(null, client.get("/spaces") { header(HttpHeaders.AcceptEncoding, "gzip") }.headers[HttpHeaders.ContentEncoding])
    }
}
