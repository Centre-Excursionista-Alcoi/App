package org.centrexcursionistalcoi.app.routes

import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
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
    private val nfc = Base64.encode(byteArrayOf(1, 2, 3, 4))

    @Test
    fun test_key_nfc_id_can_be_set_updated_and_cleared() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            SpaceEntity.new(spaceId) {
                name = "Casa"
                description = "A house"
                prices = emptyList()
            }
        },
    ) {
        val created = client.post("/space_keys") {
            contentType(ContentType.Application.Json)
            setBody("""{"space":"$spaceId","name":"Main door","nfcId":"$nfc"}""")
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
    }
}
