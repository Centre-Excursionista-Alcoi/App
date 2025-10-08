package org.centrexcursionistalcoi.app.routes

import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.*
import java.time.LocalDate
import java.util.UUID
import kotlin.test.*
import kotlin.uuid.toJavaUuid
import kotlinx.datetime.toJavaLocalDate
import kotlinx.serialization.builtins.ListSerializer
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.ResourcesUtils
import org.centrexcursionistalcoi.app.assertBody
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.data.InventoryItem
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity
import org.centrexcursionistalcoi.app.database.entity.LendingEntity
import org.centrexcursionistalcoi.app.utils.toUUID
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

class TestInventoryRoutes : ApplicationTestBase() {
    @Test
    fun test_create_type_notLoggedIn() = ProvidedRouteTests.test_notLoggedIn("/inventory/types", HttpMethod.Post)

    @Test
    fun test_create_type_notAdmin() = ProvidedRouteTests.test_loggedIn_notAdmin("/inventory/types", HttpMethod.Post)

    @Test
    fun test_create_type_invalidContentType() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN
    ) {
        client.post("/inventory/types").apply {
            assertStatusCode(HttpStatusCode.BadRequest)
        }
    }

    @Test
    fun test_create_type_missingDisplayName() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN
    ) {
        client.submitFormWithBinaryData("/inventory/types", formData = listOf()).apply {
            assertStatusCode(HttpStatusCode.BadRequest)
        }
    }

    @Test
    fun test_create_type_withDisplayName() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN
    ) {
        val location = client.submitFormWithBinaryData(
            url = "/inventory/types",
            formData = formData {
                append("displayName", "Test Item Type")
            }
        ).run {
            assertStatusCode(HttpStatusCode.Created)
            val location = headers[HttpHeaders.Location]
            assertNotNull(location)
            assertTrue(location.matches("/inventory/types/[a-z0-9-]+".toRegex()), "Location '$location' does not match expected format")
            location
        }
        val itemId = location.substringAfterLast('/').let { UUID.fromString(it) }
        Database { InventoryItemTypeEntity.findById(itemId) }.let { item ->
            assertNotNull(item)
            assertEquals("Test Item Type", item.displayName)
            assertNull(Database { item.image })
        }
        client.get(location).apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(InventoryItemType.serializer()) { item ->
                assertEquals(itemId, item.id.toJavaUuid())
                assertEquals("Test Item Type", item.displayName)
                assertNull(Database { item.image })
            }
        }
    }

    @Test
    fun test_create_type_withImage() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN
    ) {
        val imageBytes = ResourcesUtils.bytesFromResource("/square.png")
        val location = client.submitFormWithBinaryData(
            url = "/inventory/types",
            formData = formData {
                append("displayName", "Test Item Type")
                append("image", imageBytes, Headers.build {
                    append(HttpHeaders.ContentType, "image/png")
                    append(HttpHeaders.ContentDisposition, "filename=\"square.png\"")
                })
            }
        ).run {
            assertStatusCode(HttpStatusCode.Created)
            val location = headers[HttpHeaders.Location]
            assertNotNull(location)
            assertTrue(location.matches("/inventory/types/[a-z0-9-]+".toRegex()), "Location '$location' does not match expected format")
            location
        }
        val itemId = location.substringAfterLast('/').let { UUID.fromString(it) }
        Database { InventoryItemTypeEntity.findById(itemId) }.let { item ->
            assertNotNull(item)
            assertEquals("Test Item Type", item.displayName)
            Database { item.image }.let { imageFile ->
                assertNotNull(imageFile)
                assertContentEquals(imageBytes, imageFile.data)
            }
        }
        client.get(location).apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(InventoryItemType.serializer()) { item ->
                assertEquals(itemId, item.id.toJavaUuid())
                assertEquals("Test Item Type", item.displayName)
                val imageFile = item.image?.toJavaUuid()
                assertNotNull(imageFile)
                Database { FileEntity[imageFile] }.let { imageFile ->
                    assertNotNull(imageFile)
                    assertContentEquals(imageBytes, imageFile.data)
                }
            }
        }
    }

    @Test
    fun test_fetch_types() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            InventoryItemTypeEntity.new {
                displayName = "Item Type 1"
                description = "Description 1"
                image = null
            }
            val imageFile = FileEntity.new {
                name = "test_image.png"
                type = "image/png"
                data = ResourcesUtils.bytesFromResource("/square.png")
            }
            InventoryItemTypeEntity.new {
                displayName = "Item Type 2"
                description = "Description 2"
                image = imageFile
            }
        }
    ) {
        client.get("/inventory/types").apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(ListSerializer(InventoryItemType.serializer())) { items ->
                assertEquals(2, items.size)
                val item1 = items[0]
                assertNotNull(item1)
                assertEquals("Item Type 1", item1.displayName)
                assertEquals("Description 1", item1.description)
                assertNull(item1.image)
                val item2 = items[1]
                assertNotNull(item2)
                assertEquals("Item Type 2", item2.displayName)
                assertEquals("Description 2", item2.description)
                val imageFile = item2.image?.toJavaUuid()
                assertNotNull(imageFile)
                Database { FileEntity[imageFile] }.let { imageFile ->
                    assertNotNull(imageFile)
                    assertContentEquals(ResourcesUtils.bytesFromResource("/square.png"), imageFile.data)
                }
            }
        }
    }


    @Test
    fun test_create_item_notLoggedIn() = ProvidedRouteTests.test_notLoggedIn("/inventory/items", HttpMethod.Post)

    @Test
    fun test_create_item_notAdmin() = ProvidedRouteTests.test_loggedIn_notAdmin("/inventory/items", HttpMethod.Post)

    @Test
    fun test_create_item_invalidContentType() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN
    ) {
        client.post("/inventory/items").apply {
            assertStatusCode(HttpStatusCode.BadRequest)
        }
    }

    @Test
    fun test_create_item_missingType() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN
    ) {
        client.submitFormWithBinaryData("/inventory/items", formData = listOf()).apply {
            assertStatusCode(HttpStatusCode.BadRequest)
        }
    }

    @Test
    fun test_create_item_withType() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            InventoryItemTypeEntity.new {
                displayName = "Item Type 1"
                description = "Description 1"
                image = null
            }
        }
    ) { context ->
        val type = context.dibResult
        assertNotNull(type)
        val location = client.submitFormWithBinaryData(
            url = "/inventory/items",
            formData = formData {
                append("type", type.id.value.toString())
            }
        ).run {
            assertStatusCode(HttpStatusCode.Created)
            val location = headers[HttpHeaders.Location]
            assertNotNull(location)
            assertTrue(location.matches("/inventory/items/[a-z0-9-]+".toRegex()), "Location '$location' does not match expected format")
            location
        }
        val itemId = location.substringAfterLast('/').let { UUID.fromString(it) }
        Database { InventoryItemEntity.findById(itemId) }.let { item ->
            assertNotNull(item)
            assertNull(item.variation)
            assertEquals(type.id.value, Database { item.type.id.value })
        }
        client.get(location).apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(InventoryItem.serializer()) { item ->
                assertEquals(itemId, item.id.toJavaUuid())
                assertNull(item.variation)
                assertEquals(type.id.value, item.type.toJavaUuid())
            }
        }
    }

    @Test
    fun test_create_item_withVariant() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            InventoryItemTypeEntity.new {
                displayName = "Item Type 1"
                description = "Description 1"
                image = null
            }
        }
    ) { context ->
        val type = context.dibResult
        assertNotNull(type)
        val location = client.submitFormWithBinaryData(
            url = "/inventory/items",
            formData = formData {
                append("type", type.id.value.toString())
                append("variation", "Variant A")
            }
        ).run {
            assertStatusCode(HttpStatusCode.Created)
            val location = headers[HttpHeaders.Location]
            assertNotNull(location)
            assertTrue(location.matches("/inventory/items/[a-z0-9-]+".toRegex()), "Location '$location' does not match expected format")
            location
        }
        val itemId = location.substringAfterLast('/').let { UUID.fromString(it) }
        Database { InventoryItemEntity.findById(itemId) }.let { item ->
            assertNotNull(item)
            assertEquals("Variant A", item.variation)
            assertEquals(type.id.value, Database { item.type.id.value })
        }
        client.get(location).apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(InventoryItem.serializer()) { item ->
                assertEquals(itemId, item.id.toJavaUuid())
                assertEquals("Variant A", item.variation)
                assertEquals(type.id.value, item.type.toJavaUuid())
            }
        }
    }


    @Test
    fun test_create_lending_notLoggedIn() = ProvidedRouteTests.test_notLoggedIn("/inventory/items/123/lendings", HttpMethod.Post)

    @Test
    fun test_create_lending_malformedId() = runApplicationTest(
        shouldLogIn = LoginType.USER
    ) {
        client.post("/inventory/items/invalid-uuid/lendings").apply {
            assertStatusCode(HttpStatusCode.BadRequest)
        }
    }

    @Test
    fun test_create_lending_itemNotFound() = runApplicationTest(
        shouldLogIn = LoginType.USER
    ) {
        client.post("/inventory/items/00000000-0000-0000-0000-000000000000/lendings").apply {
            assertStatusCode(HttpStatusCode.NotFound)
        }
    }

    private val exampleItemId = "6900c106-2f54-4c22-a3c4-6260a50961e6".toUUID()
    private fun JdbcTransaction.initializeItem(): InventoryItemEntity {
        val itemType = InventoryItemTypeEntity.new {
            displayName = "Item Type 1"
            description = "Description 1"
            image = null
        }
        return InventoryItemEntity.new(exampleItemId) {
            variation = "Variant A"
            type = itemType
        }
    }

    @Test
    fun test_create_lending_invalidContentType() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { initializeItem() },
    ) {
        client.post("/inventory/items/$exampleItemId/lendings").apply {
            assertStatusCode(HttpStatusCode.BadRequest)
        }
    }

    @Test
    fun test_create_lending_missingParameters() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { initializeItem() },
    ) {
        // No parameters
        client.submitForm("/inventory/items/$exampleItemId/lendings").apply {
            assertStatusCode(HttpStatusCode.BadRequest)
        }
        // Missing 'from' parameter
        client.submitForm(
            "/inventory/items/$exampleItemId/lendings",
            parameters {
                // append("from", "2024-01-01")
                append("to", "2024-01-10")
                append("items", "$exampleItemId")
            }
        ).apply {
            assertStatusCode(HttpStatusCode.BadRequest)
        }
        // Malformed 'from' parameter
        client.submitForm(
            "/inventory/items/$exampleItemId/lendings",
            parameters {
                append("from", "abc")
                append("to", "2024-01-10")
                append("items", "$exampleItemId")
            }
        ).apply {
            assertStatusCode(HttpStatusCode.BadRequest)
        }
        // Missing 'to' parameter
        client.submitForm(
            "/inventory/items/$exampleItemId/lendings",
            parameters {
                append("from", "2024-01-01")
                // append("to", "2024-01-10")
                append("items", "$exampleItemId")
            }
        ).apply {
            assertStatusCode(HttpStatusCode.BadRequest)
        }
        // Malformed 'to' parameter
        client.submitForm(
            "/inventory/items/$exampleItemId/lendings",
            parameters {
                append("from", "2024-01-10")
                append("to", "abc")
                append("items", "$exampleItemId")
            }
        ).apply {
            assertStatusCode(HttpStatusCode.BadRequest)
        }
        // Missing 'items' parameter
        client.submitForm(
            "/inventory/items/$exampleItemId/lendings",
            parameters {
                append("from", "2024-01-01")
                append("to", "2024-01-10")
                // append("items", "$exampleItemId")
            }
        ).apply {
            assertStatusCode(HttpStatusCode.BadRequest)
        }
        // Malformed 'items' parameter
        client.submitForm(
            "/inventory/items/$exampleItemId/lendings",
            parameters {
                append("from", "2024-01-01")
                append("to", "2024-01-10")
                append("items", "abc")
            }
        ).apply {
            assertStatusCode(HttpStatusCode.BadRequest)
        }
        // Non-existing single item id
        client.submitForm(
            "/inventory/items/$exampleItemId/lendings",
            parameters {
                append("from", "2024-01-01")
                append("to", "2024-01-10")
                append("items", "c2de5abe-a290-4847-adee-14a81d5349ae")
            }
        ).apply {
            assertStatusCode(HttpStatusCode.BadRequest)
        }
    }

    @Test
    fun test_create_lending_correct() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { initializeItem() },
    ) { context ->
        val item = context.dibResult
        assertNotNull(item)

        // The reference user must exist in the database
        Database { FakeUser.provideEntity() }

        // Single item
        val location = client.submitForm(
            "/inventory/items/$exampleItemId/lendings",
            parameters {
                append("from", "2024-01-01")
                append("to", "2024-01-10")
                append("items", exampleItemId.toString())
                append("notes", "These are some notes")
            }
        ).run {
            assertStatusCode(HttpStatusCode.Created)
            val location = headers[HttpHeaders.Location]
            assertNotNull(location)
            assertTrue("Unexpected format for location: $location") {
                location.matches("/inventory/items/$exampleItemId/lendings/[0-9a-f-]+".toRegex())
            }
            location
        }
        val lendingId = location.substringAfterLast('/').toUUID()
        Database { LendingEntity.findById(lendingId) }.let { lending ->
            assertNotNull(lending)
            val items = Database { lending.items.toList() }
            assertEquals(1, items.size)
            assertEquals(item.id.value, items[0].id.value)
        }
        client.get(location).apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(Lending.serializer()) { lending ->
                assertEquals(lendingId, lending.id.toJavaUuid())
                assertEquals(FakeUser.SUB, lending.userSub)
                assertEquals(LocalDate.of(2024, 1, 1), lending.from.toJavaLocalDate())
                assertEquals(LocalDate.of(2024, 1, 10), lending.to.toJavaLocalDate())
                assertEquals("These are some notes", lending.notes)
                assertEquals(1, lending.items.size)
                assertEquals(item.id.value, lending.items[0].id.toJavaUuid())
            }
        }
    }
}
