package org.centrexcursionistalcoi.app.routes

import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.database.entity.SpaceEntity
import org.centrexcursionistalcoi.app.test.LoginType
import org.centrexcursionistalcoi.app.utils.toUuid

class TestSpaceKeysRoutes : ApplicationTestBase() {
    private val spaceId = "5c1c3f0e-0b0a-4c3a-9d55-0a1b2c3d4e5f".toUuid()
    private val otherSpaceId = "5c1c3f0e-0b0a-4c3a-9d55-0a1b2c3d4e60".toUuid()
    private val nfc = Base64.encode(byteArrayOf(1, 2, 3, 4))

    private fun createSpaces() {
        SpaceEntity.new(spaceId) { name = "Casa"; description = "A house"; prices = emptyList() }
        SpaceEntity.new(otherSpaceId) { name = "Refugi"; description = "A shelter"; prices = emptyList() }
    }

    @Test
    fun test_key_types_can_be_for_several_spaces_and_changed() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = { createSpaces() },
    ) {
        val created = client.post("/space_key_types") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Car permit","spaces":[{"space":"$spaceId","maxPerLending":1},{"space":"$otherSpaceId","maxPerLending":2}]}""")
        }
        created.assertStatusCode(HttpStatusCode.Created)
        val location = created.headers["Location"]!!

        suspend fun fetch() = Json.parseToJsonElement(client.get(location).bodyAsText()).jsonObject
        assertEquals(2, fetch().getValue("spaces").jsonArray.size)

        // The spaces are replaced
        client.patch(location) {
            contentType(ContentType.Application.Json)
            setBody("""{"spaces":[{"space":"$otherSpaceId","maxPerLending":4}]}""")
        }.assertStatusCode(HttpStatusCode.OK)
        fetch().getValue("spaces").jsonArray.single().jsonObject.apply {
            assertEquals("$otherSpaceId", getValue("space").jsonPrimitive.content)
            assertEquals("4", getValue("maxPerLending").jsonPrimitive.content)
        }

        // A space that doesn't exist, or a maximum that makes no sense
        client.post("/space_key_types") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Bad","spaces":[{"space":"${"7c1c3f0e-0b0a-4c3a-9d55-0a1b2c3d4e5f"}","maxPerLending":1}]}""")
        }.assertStatusCode(HttpStatusCode.NotFound)
        client.post("/space_key_types") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Bad","spaces":[{"space":"$spaceId","maxPerLending":0}]}""")
        }.assertStatusCode(HttpStatusCode.BadRequest)
    }

    @Test
    fun test_key_nfc_id_can_be_set_updated_and_cleared() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = { createSpaces() },
    ) {
        val type = client.post("/space_key_types") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Main door","spaces":[{"space":"$spaceId","maxPerLending":1}]}""")
        }.headers["Location"]!!.substringAfterLast('/')

        val created = client.post("/space_keys") {
            contentType(ContentType.Application.Json)
            setBody("""{"type":"$type","label":"A","nfcId":"$nfc"}""")
        }
        created.assertStatusCode(HttpStatusCode.Created)
        val location = created.headers["Location"]!!

        suspend fun fetch() = Json.parseToJsonElement(client.get(location).bodyAsText()).jsonObject
        assertEquals(nfc, fetch().getValue("nfcId").jsonPrimitive.content)
        assertEquals(1, (Json.parseToJsonElement(client.get("/space_keys").bodyAsText()) as JsonArray).size)

        val other = Base64.encode(byteArrayOf(9, 9))
        client.patch(location) {
            contentType(ContentType.Application.Json)
            setBody("""{"nfcId":"$other"}""")
        }.assertStatusCode(HttpStatusCode.OK)
        assertEquals(other, fetch().getValue("nfcId").jsonPrimitive.content)

        // An empty id removes the tag
        client.patch(location) {
            contentType(ContentType.Application.Json)
            setBody("""{"nfcId":""}""")
        }.assertStatusCode(HttpStatusCode.OK)
        assertNull(fetch()["nfcId"])

        // A type with keys cannot be deleted
        client.delete("/space_key_types/$type").assertStatusCode(HttpStatusCode.Conflict)
    }

    @Test
    fun test_regular_users_see_the_types_but_not_the_keys() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            createSpaces()
            val type = org.centrexcursionistalcoi.app.database.entity.SpaceKeyTypeEntity.new { name = "Door" }
            org.centrexcursionistalcoi.app.database.entity.SpaceKeyEntity.new { this.type = type }
        },
    ) {
        assertEquals(1, (Json.parseToJsonElement(client.get("/space_key_types").bodyAsText()) as JsonArray).size)
        assertEquals(0, (Json.parseToJsonElement(client.get("/space_keys").bodyAsText()) as JsonArray).size)
    }
}
