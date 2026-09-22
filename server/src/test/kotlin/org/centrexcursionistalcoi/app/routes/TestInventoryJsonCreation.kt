package org.centrexcursionistalcoi.app.routes

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.uuid.toKotlinUuid
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertBody
import org.centrexcursionistalcoi.app.assertError
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.data.DepartmentRole
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.data.InventoryItem
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity
import org.centrexcursionistalcoi.app.database.table.DepartmentMembers
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.request.CreateInventoryItemRequest
import org.centrexcursionistalcoi.app.request.CreateInventoryItemTypeRequest
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.test.LoginType
import org.jetbrains.exposed.v1.jdbc.insert

/**
 * `POST /inventory/types` and `POST /inventory/items` accepting a JSON body (#659), same pattern as
 * `TestPostsJsonCreation`. The multipart path stays covered by `TestRoutes.kt`'s generic `runTestsOnRoute`
 * harness, unaffected by any of this.
 */
class TestInventoryJsonCreation : ApplicationTestBase() {
    private suspend fun HttpClient.postTypeJson(request: CreateInventoryItemTypeRequest) = post("/inventory/types") {
        contentType(ContentType.Application.Json)
        setBody(json.encodeToString(CreateInventoryItemTypeRequest.serializer(), request))
    }

    private suspend fun HttpClient.postItemJson(request: CreateInventoryItemRequest) = post("/inventory/items") {
        contentType(ContentType.Application.Json)
        setBody(json.encodeToString(CreateInventoryItemRequest.serializer(), request))
    }

    // ---- Item types ----

    @Test
    fun test_createType_json_notLoggedIn_unauthorized() = runApplicationTest {
        client.postTypeJson(CreateInventoryItemTypeRequest(displayName = "Type")).assertStatusCode(HttpStatusCode.Unauthorized)
    }

    @Test
    fun test_createType_json_loggedIn_noRole_forbidden() = runApplicationTest(shouldLogIn = LoginType.USER) {
        client.postTypeJson(CreateInventoryItemTypeRequest(displayName = "Type")).assertStatusCode(HttpStatusCode.Forbidden)
    }

    @Test
    fun test_createType_json_malformedBody_badRequest() = runApplicationTest(shouldLogIn = LoginType.ADMIN) {
        client.post("/inventory/types") {
            contentType(ContentType.Application.Json)
            setBody("not json")
        }.assertStatusCode(HttpStatusCode.BadRequest)
    }

    @Test
    fun test_createType_json_missingRequiredField_badRequest() = runApplicationTest(shouldLogIn = LoginType.ADMIN) {
        client.post("/inventory/types") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }.assertStatusCode(HttpStatusCode.BadRequest)
    }

    @Test
    fun test_createType_json_requiredFieldsOnly_persistsAndIsReadableBack() = runApplicationTest(shouldLogIn = LoginType.ADMIN) {
        val location = client.postTypeJson(CreateInventoryItemTypeRequest(displayName = "JSON Type")).run {
            assertStatusCode(HttpStatusCode.Created)
            val location = headers[HttpHeaders.Location]
            assertNotNull(location)
            location
        }

        client.get(location).apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(InventoryItemType.serializer()) { type ->
                assertEquals("JSON Type", type.displayName)
                assertNull(type.description)
                assertNull(type.image)
            }
        }
    }

    @Test
    fun test_createType_json_withDepartmentAndImage_persistsEverything() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = { DepartmentEntity.new { displayName = "JSON Department" } },
    ) { context ->
        val department = context.dibResult!!
        val imageBytes = "fake png bytes".encodeToByteArray()

        // Not covering `categories` here: reading it back via GET hits a pre-existing, unrelated bug (an Exposed
        // array/text[] column read outside the transaction that fetched the row, same class of issue as
        // AGENTS.md already documents for DepartmentMemberEntity.roles) -- not something this PR introduced or
        // is in scope to fix, flagged separately.
        val location = client.postTypeJson(
            CreateInventoryItemTypeRequest(
                displayName = "JSON Type With Extras",
                description = "A description",
                weight = 3.5,
                department = department.id.value.toKotlinUuid(),
                image = FileWithContext(bytes = imageBytes, name = "type.png", contentType = ContentType.Image.PNG),
            )
        ).run {
            assertStatusCode(HttpStatusCode.Created)
            val location = headers[HttpHeaders.Location]
            assertNotNull(location)
            location
        }

        client.get(location).apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(InventoryItemType.serializer()) { type ->
                assertEquals("A description", type.description)
                assertEquals(3.5, type.weight)
                assertEquals(department.id.value.toString(), type.department.toString())
                assertNotNull(type.image)
            }
        }

        val storedImage = Database { FileEntity.all().first() }
        val storedBytes = Database { storedImage.bytes }
        assertEquals(imageBytes.toList(), storedBytes.toList())
    }

    @Test
    fun test_createType_json_inventoryManager_otherDepartment_rejectedAndDoesNotOrphanImage() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            FakeUser.provideEntity()
            val managed = DepartmentEntity.new { displayName = "Managed Department" }
            val other = DepartmentEntity.new { displayName = "Other Department" }
            DepartmentMembers.insert {
                it[userSub] = FakeUser.SUB
                it[departmentId] = managed.id
                it[confirmed] = true
                it[roles] = listOf(DepartmentRole.INVENTORY_MANAGER.storageName)
            }
            other
        },
    ) { context ->
        val otherDepartment = context.dibResult!!

        client.postTypeJson(
            CreateInventoryItemTypeRequest(
                displayName = "Cross-department type",
                department = otherDepartment.id.value.toKotlinUuid(),
                image = FileWithContext(bytes = "img".encodeToByteArray(), name = "x.png"),
            )
        ).assertError(Error.PermissionRejected())

        val remainingTypes = Database { InventoryItemTypeEntity.all().toList() }
        val remainingFiles = Database { FileEntity.all().toList() }
        assertEquals(0, remainingTypes.size, "Rejected type creation should have been rolled back")
        assertEquals(0, remainingFiles.size, "Rejected type creation should not leave an orphaned image behind")
    }

    // ---- Items ----

    @Test
    fun test_createItem_json_missingRequiredField_badRequest() = runApplicationTest(shouldLogIn = LoginType.ADMIN) {
        client.post("/inventory/items") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }.assertStatusCode(HttpStatusCode.BadRequest)
    }

    @Test
    fun test_createItem_json_malformedBody_badRequest() = runApplicationTest(shouldLogIn = LoginType.ADMIN) {
        client.post("/inventory/items") {
            contentType(ContentType.Application.Json)
            setBody("not json")
        }.assertStatusCode(HttpStatusCode.BadRequest)
    }

    @Test
    fun test_createItem_json_typeDoesNotExist_notFound() = runApplicationTest(shouldLogIn = LoginType.ADMIN) {
        client.postItemJson(CreateInventoryItemRequest(type = kotlin.uuid.Uuid.random())).assertStatusCode(HttpStatusCode.NotFound)
    }

    @Test
    fun test_createItem_json_requiredFieldsOnly_persistsAndIsReadableBack() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = { InventoryItemTypeEntity.new { displayName = "Item Type" } },
    ) { context ->
        val type = context.dibResult!!

        val location = client.postItemJson(CreateInventoryItemRequest(type = type.id.value.toKotlinUuid())).run {
            assertStatusCode(HttpStatusCode.Created)
            val location = headers[HttpHeaders.Location]
            assertNotNull(location)
            location
        }

        client.get(location).apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(InventoryItem.serializer()) { item ->
                assertEquals(type.id.value.toString(), item.type.toString())
                assertNull(item.variation)
                assertNull(item.nfcId)
            }
        }
    }

    @Test
    fun test_createItem_json_withOptionalFields_persistsEverything() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = { InventoryItemTypeEntity.new { displayName = "Item Type" } },
    ) { context ->
        val type = context.dibResult!!
        val nfc = byteArrayOf(1, 2, 3, 4)

        val location = client.postItemJson(
            CreateInventoryItemRequest(
                type = type.id.value.toKotlinUuid(),
                variation = "Large",
                nfcId = nfc,
                manufacturerTraceabilityCode = "TRC-123",
            )
        ).run {
            assertStatusCode(HttpStatusCode.Created)
            val location = headers[HttpHeaders.Location]
            assertNotNull(location)
            location
        }

        client.get(location).apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(InventoryItem.serializer()) { item ->
                assertEquals("Large", item.variation)
                assertEquals(nfc.toList(), item.nfcId?.toList())
                assertEquals("TRC-123", item.manufacturerTraceabilityCode)
            }
        }
    }

    @Test
    fun test_createItem_json_inventoryManager_otherDepartmentsType_rejected() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            FakeUser.provideEntity()
            val managed = DepartmentEntity.new { displayName = "Managed Department" }
            val other = DepartmentEntity.new { displayName = "Other Department" }
            DepartmentMembers.insert {
                it[userSub] = FakeUser.SUB
                it[departmentId] = managed.id
                it[confirmed] = true
                it[roles] = listOf(DepartmentRole.INVENTORY_MANAGER.storageName)
            }
            InventoryItemTypeEntity.new {
                displayName = "Other department's type"
                department = other
            }
        },
    ) { context ->
        val otherType = context.dibResult!!

        client.postItemJson(CreateInventoryItemRequest(type = otherType.id.value.toKotlinUuid()))
            .assertError(Error.PermissionRejected())

        val remainingItems = Database { InventoryItemEntity.all().toList() }
        assertEquals(0, remainingItems.size, "Rejected item creation should have been rolled back")
    }
}
