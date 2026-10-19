package org.centrexcursionistalcoi.app.routes

import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.data.SpaceClimateControl
import org.centrexcursionistalcoi.app.data.SpaceDetail
import org.centrexcursionistalcoi.app.data.SpaceEnergySource
import org.centrexcursionistalcoi.app.request.CreateSpaceRequest
import org.centrexcursionistalcoi.app.request.UpdateSpaceRequest
import org.centrexcursionistalcoi.app.test.LoginType
import org.centrexcursionistalcoi.app.utils.requestWithFilesBody

class TestSpaceDetailsRoutes : ApplicationTestBase() {
    private val photo = FileWithContext(bytes = byteArrayOf(1, 2, 3), name = "inside.jpg", contentType = ContentType.Image.JPEG)

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.fetch(location: String): JsonObject =
        Json.parseToJsonElement(client.get(location).bodyAsText()).jsonObject

    @Test
    fun test_specs_location_and_photos_are_stored_and_returned() = runApplicationTest(shouldLogIn = LoginType.ADMIN) {
        val request = CreateSpaceRequest(
            name = "Refugi",
            description = "A shelter",
            photos = listOf(photo),
            seats = 20,
            beds = 18,
            energySources = listOf(SpaceEnergySource.SOLAR, SpaceEnergySource.DIESEL_GENERATOR, SpaceEnergySource.SOLAR),
            climateControl = listOf(SpaceClimateControl.WOOD_STOVE),
            address = "  Carrer Major 1  ",
            latitude = 38.7,
            longitude = -0.48,
            altitude = 1250.0,
        )
        val created = client.post("/spaces") {
            setBody(requestWithFilesBody(request, CreateSpaceRequest.serializer()))
        }
        created.assertStatusCode(HttpStatusCode.Created)
        val space = fetch(created.headers["Location"]!!)

        assertEquals("20", space.getValue("seats").jsonPrimitive.content)
        assertEquals("18", space.getValue("beds").jsonPrimitive.content)
        assertEquals(listOf("SOLAR", "DIESEL_GENERATOR"), space.getValue("energySources").jsonArray.map { it.jsonPrimitive.content })
        assertEquals(listOf("WOOD_STOVE"), space.getValue("climateControl").jsonArray.map { it.jsonPrimitive.content })
        assertEquals("Carrer Major 1", space.getValue("address").jsonPrimitive.content)
        assertEquals("38.7", space.getValue("latitude").jsonPrimitive.content)
        assertEquals("1250.0", space.getValue("altitude").jsonPrimitive.content)
        val photos = space.getValue("photos").jsonArray
        assertEquals(1, photos.size)
        val photoId = photos.single().jsonObject.getValue("id").jsonPrimitive.content
        assertEquals(HttpStatusCode.OK, client.get("/download/$photoId").status)
    }

    @Test
    fun test_several_photos_are_all_stored() = runApplicationTest(shouldLogIn = LoginType.ADMIN) {
        val photos = (1..5).map { FileWithContext(bytes = byteArrayOf(it.toByte(), 2, 3), name = "p$it.jpg", contentType = ContentType.Image.JPEG) }
        val created = client.post("/spaces") {
            setBody(requestWithFilesBody(CreateSpaceRequest(name = "Refugi", description = "A shelter", photos = photos), CreateSpaceRequest.serializer()))
        }
        created.assertStatusCode(HttpStatusCode.Created)
        assertEquals(5, fetch(created.headers["Location"]!!).getValue("photos").jsonArray.size)
    }

    @Test
    fun test_details_can_be_changed_cleared_and_photos_removed() = runApplicationTest(shouldLogIn = LoginType.ADMIN) {
        val created = client.post("/spaces") {
            setBody(
                requestWithFilesBody(
                    CreateSpaceRequest(
                        name = "Refugi", description = "A shelter", photos = listOf(photo),
                        seats = 20, beds = 18, latitude = 38.7, longitude = -0.48, altitude = 900.0, address = "Somewhere",
                    ),
                    CreateSpaceRequest.serializer(),
                )
            )
        }
        val location = created.headers["Location"]!!
        val photoId = fetch(location).getValue("photos").jsonArray.single().jsonObject.getValue("id").jsonPrimitive.content

        suspend fun patch(request: UpdateSpaceRequest) = client.patch(location) {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(UpdateSpaceRequest.serializer(), request))
        }

        patch(UpdateSpaceRequest(beds = 30, energySources = listOf(SpaceEnergySource.GRID))).assertStatusCode(HttpStatusCode.OK)
        fetch(location).apply {
            assertEquals("30", getValue("beds").jsonPrimitive.content)
            assertEquals("20", getValue("seats").jsonPrimitive.content)
            assertEquals(listOf("GRID"), getValue("energySources").jsonArray.map { it.jsonPrimitive.content })
        }

        // Clearing, and an empty address
        patch(UpdateSpaceRequest(address = "", clear = setOf(SpaceDetail.SEATS, SpaceDetail.COORDINATES, SpaceDetail.ALTITUDE)))
            .assertStatusCode(HttpStatusCode.OK)
        fetch(location).apply {
            for (key in listOf("seats", "latitude", "longitude", "altitude", "address")) {
                assertTrue(key !in keys || getValue(key).toString() == "null", "$key should be cleared")
            }
            assertEquals("30", getValue("beds").jsonPrimitive.content)
        }

        // A photo is removed by sending only its id
        patch(UpdateSpaceRequest(photos = listOf(FileWithContext(id = kotlin.uuid.Uuid.parse(photoId))))).assertStatusCode(HttpStatusCode.OK)
        assertEquals(0, fetch(location).getValue("photos").jsonArray.size)
    }

    @Test
    fun test_nonsensical_details_are_rejected() = runApplicationTest(shouldLogIn = LoginType.ADMIN) {
        suspend fun create(request: CreateSpaceRequest) = client.post("/spaces") {
            setBody(requestWithFilesBody(request, CreateSpaceRequest.serializer()))
        }
        val base = CreateSpaceRequest(name = "Refugi", description = "A shelter")

        create(base.copy(seats = -1)).assertStatusCode(HttpStatusCode.BadRequest)
        create(base.copy(latitude = 91.0, longitude = 0.0)).assertStatusCode(HttpStatusCode.BadRequest)
        // Coordinates come in pairs
        create(base.copy(latitude = 10.0)).assertStatusCode(HttpStatusCode.BadRequest)
        create(base.copy(altitude = 20000.0)).assertStatusCode(HttpStatusCode.BadRequest)

        val location = create(base).headers["Location"]!!
        client.patch(location) {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(UpdateSpaceRequest.serializer(), UpdateSpaceRequest(latitude = 10.0)))
        }.assertStatusCode(HttpStatusCode.BadRequest)
        // The failed patch changed nothing
        assertEquals(null, fetch(location)["latitude"]?.jsonPrimitive?.content?.takeUnless { it == "null" })
    }
}
