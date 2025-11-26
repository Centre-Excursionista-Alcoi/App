package org.centrexcursionistalcoi.app.routes

import io.ktor.client.request.delete
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.parameters
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid
import kotlinx.datetime.toJavaLocalDate
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.assertBody
import org.centrexcursionistalcoi.app.assertError
import org.centrexcursionistalcoi.app.assertStatusCode
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity
import org.centrexcursionistalcoi.app.database.entity.LendingEntity
import org.centrexcursionistalcoi.app.database.entity.LendingUserEntity
import org.centrexcursionistalcoi.app.database.entity.ReceivedItemEntity
import org.centrexcursionistalcoi.app.database.entity.UserInsuranceEntity
import org.centrexcursionistalcoi.app.database.table.LendingItems
import org.centrexcursionistalcoi.app.database.table.ReceivedItems
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.request.ReturnLendingRequest
import org.centrexcursionistalcoi.app.serialization.list
import org.centrexcursionistalcoi.app.test.*
import org.centrexcursionistalcoi.app.today
import org.centrexcursionistalcoi.app.utils.toUUID
import org.centrexcursionistalcoi.app.utils.toUUIDOrNull
import org.centrexcursionistalcoi.app.utils.toUuid
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insert

class TestLendingsRoutes : ApplicationTestBase() {

    @Test
    fun test_create_lending_notLoggedIn() = ProvidedRouteTests.test_notLoggedIn("/inventory/lendings", HttpMethod.Post)

    private val exampleItemTypeId = "66868070-47fe-4c2f-8fca-484ef6dee119".toUUID()
    private val exampleItemId = "6900c106-2f54-4c22-a3c4-6260a50961e6".toUUID()
    context(_: JdbcTransaction)
    private fun getOrCreateItem(
        id: UUID = exampleItemId,
        variation: String? = "Variant A",
        type: InventoryItemTypeEntity = getOrCreateItemType()
    ): InventoryItemEntity {
        return InventoryItemEntity.findById(id) ?: InventoryItemEntity.new(id) {
            this.variation = variation
            this.type = type
        }
    }

    context(_: JdbcTransaction)
    private fun getOrCreateItemType(
        id: UUID = exampleItemTypeId,
        displayName: String = "Item Type 1",
        description: String? = "Description 1",
        image: FileEntity? = null,
    ): InventoryItemTypeEntity = InventoryItemTypeEntity.findById(id) ?: InventoryItemTypeEntity.new(id) {
        this.displayName = displayName
        this.description = description
        this.image = image
    }

    @Test
    fun test_create_lending_invalidContentType() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { getOrCreateItem() },
    ) {
        client.post("/inventory/lendings").apply {
            assertStatusCode(HttpStatusCode.BadRequest)
        }
    }

    @Test
    fun test_create_lending_missingParameters() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { getOrCreateItem() },
    ) {
        // No parameters
        client.submitForm("/inventory/lendings").apply {
            assertStatusCode(HttpStatusCode.BadRequest)
        }
        // Missing 'from' parameter
        client.submitForm(
            "/inventory/lendings",
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
            "/inventory/lendings",
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
            "/inventory/lendings",
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
            "/inventory/lendings",
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
            "/inventory/lendings",
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
            "/inventory/lendings",
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
            "/inventory/lendings",
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
    fun test_create_lending_datesInPast() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { getOrCreateItem() },
        finally = { today = { LocalDate.now() } },
    ) {
        today = { LocalDate.of(2025, 10, 8) }

        // Both dates in past
        client.submitForm(
            "/inventory/lendings",
            parameters {
                append("from", "2025-10-04")
                append("to", "2025-10-05")
                append("items", exampleItemId.toString())
            }
        ).apply {
            assertStatusCode(HttpStatusCode.BadRequest)
        }
        // 'from' date in past
        client.submitForm(
            "/inventory/lendings",
            parameters {
                append("from", "2025-10-04")
                append("to", "2025-10-10")
                append("items", exampleItemId.toString())
            }
        ).apply {
            assertStatusCode(HttpStatusCode.BadRequest)
        }
    }

    @Test
    fun test_create_lending_notSignedUpForLendings() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            getOrCreateItem()

            val user = FakeUser.provideEntity()

            // Register user for lendings
            Database {
                LendingUserEntity.new {
                    userSub = user.sub
                    phoneNumber = "123456789"
                    sports = listOf(Sports.HIKING)
                }
            }
        },
        finally = { today = { LocalDate.now() } },
    ) {
        today = { LocalDate.of(2025, 10, 1) }

        // New lending starting on the day existing one starts
        client.submitForm(
            "/inventory/lendings",
            parameters {
                append("from", "2025-10-10")
                append("to", "2025-10-12")
                append("items", exampleItemId.toString())
            }
        ).apply {
            assertError(Error.UserDoesNotHaveInsurance())
        }
    }

    @Test
    fun test_create_lending_noInsurance() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            getOrCreateItem()

            FakeUser.provideEntity()
        },
        finally = { today = { LocalDate.now() } },
    ) {
        today = { LocalDate.of(2025, 10, 1) }

        // New lending starting on the day existing one starts
        client.submitForm(
            "/inventory/lendings",
            parameters {
                append("from", "2025-10-10")
                append("to", "2025-10-12")
                append("items", exampleItemId.toString())
            }
        ).apply {
            assertError(Error.UserDoesNotHaveInsurance())
        }
    }

    @Test
    fun test_create_lending_conflicts() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            getOrCreateItem()

            val user = FakeUser.provideEntity()

            // Existing lending from 2025-10-10 to 2025-10-15
            val adminUser = FakeAdminUser.provideEntity()
            LendingEntity.new {
                this.userSub = adminUser
                this.from = LocalDate.of(2025, 10, 10)
                this.to = LocalDate.of(2025, 10, 15)
                this.notes = "Existing lending"
            }.also { lendingEntity ->
                LendingItems.insert {
                    it[item] = exampleItemId
                    it[lending] = lendingEntity.id
                }
            }

            // Register user for lendings
            Database {
                LendingUserEntity.new {
                    userSub = user.sub
                    phoneNumber = "123456789"
                    sports = listOf(Sports.HIKING)
                }
            }

            // Add insurance for the user
            UserInsuranceEntity.new {
                userSub = user
                validFrom = LocalDate.of(2025, 1, 1)
                validTo = LocalDate.of(2025, 12, 31)
                insuranceCompany = "Insurance Co"
                policyNumber = "POL123456"
            }
        },
        finally = { today = { LocalDate.now() } },
    ) {
        today = { LocalDate.of(2025, 10, 1) }

        fun HttpResponse.delete() {
            val location = headers[HttpHeaders.Location]
            assertNotNull(location, "Missing Location header in response")
            val id = location.substringAfterLast('/').toUUIDOrNull()
            assertNotNull(id, "Invalid UUID in Location header: $location")
            Database { LendingEntity[id].delete() }
        }

        // New lending completely before existing one
        client.submitForm(
            "/inventory/lendings",
            parameters {
                append("from", "2025-10-05")
                append("to", "2025-10-09")
                append("items", exampleItemId.toString())
            }
        ).apply {
            assertStatusCode(HttpStatusCode.Created)
            delete()
        }
        // New lending starting on the day existing one starts
        client.submitForm(
            "/inventory/lendings",
            parameters {
                append("from", "2025-10-10")
                append("to", "2025-10-12")
                append("items", exampleItemId.toString())
            }
        ).apply {
            assertStatusCode(HttpStatusCode.Conflict)
        }
        // New lending starting during existing one
        client.submitForm(
            "/inventory/lendings",
            parameters {
                append("from", "2025-10-12")
                append("to", "2025-10-18")
                append("items", exampleItemId.toString())
            }
        ).apply {
            assertStatusCode(HttpStatusCode.Conflict)
        }
        // New lending ending on the day existing one ends
        client.submitForm(
            "/inventory/lendings",
            parameters {
                append("from", "2025-10-14")
                append("to", "2025-10-15")
                append("items", exampleItemId.toString())
            }
        ).apply {
            assertStatusCode(HttpStatusCode.Conflict)
        }
        // New lending completely after existing one
        client.submitForm(
            "/inventory/lendings",
            parameters {
                append("from", "2025-10-16")
                append("to", "2025-10-20")
                append("items", exampleItemId.toString())
            }
        ).apply {
            assertStatusCode(HttpStatusCode.Created)
        }
    }

    @Test
    fun test_create_lending_pendingMemory() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            getOrCreateItem()

            // Existing lending from 2025-10-01 to 2025-10-03
            val user = FakeUser.provideEntity()
            LendingEntity.new {
                this.userSub = user
                this.from = LocalDate.of(2025, 10, 1)
                this.to = LocalDate.of(2025, 10, 3)

                this.confirmed = true
                this.taken = true
                this.givenBy = FakeAdminUser.provideEntity().sub
                this.givenAt = LocalDate.of(2025, 9, 30).atStartOfDay().toInstant(ZoneOffset.UTC)
                this.returned = true

                this.notes = "Example lending"
            }.also { lendingEntity ->
                LendingItems.insert {
                    it[item] = exampleItemId
                    it[lending] = lendingEntity.id
                }
            }.also { lendingEntity ->
                ReceivedItemEntity.new {
                    this.lending = lendingEntity
                    this.item = InventoryItemEntity[exampleItemId]
                    this.receivedBy = FakeAdminUser.provideEntity()
                    this.receivedAt = LocalDate.of(2025, 10, 4).atStartOfDay().toInstant(ZoneOffset.UTC)
                }
            }

            // Register user for lendings
            Database {
                LendingUserEntity.new {
                    userSub = user.sub
                    phoneNumber = "123456789"
                    sports = listOf(Sports.HIKING)
                }
            }

            // Add insurance for the user
            UserInsuranceEntity.new {
                userSub = user
                validFrom = LocalDate.of(2025, 1, 1)
                validTo = LocalDate.of(2025, 12, 31)
                insuranceCompany = "Insurance Co"
                policyNumber = "POL123456"
            }
        },
        finally = { today = { LocalDate.now() } },
    ) {
        today = { LocalDate.of(2025, 10, 5) }

        // New lending without having submitted memory for previous lending
        client.submitForm(
            "/inventory/lendings",
            parameters {
                append("from", "2025-10-10")
                append("to", "2025-10-12")
                append("items", exampleItemId.toString())
            }
        ).apply {
            assertError(Error.MemoryNotSubmitted())
        }
    }

    @Test
    fun test_create_lending_correct() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = { getOrCreateItem() },
        finally = { today = { LocalDate.now() } },
    ) { context ->
        val item = context.dibResult
        assertNotNull(item)

        // The reference user must exist in the database
        val user = Database { FakeUser.provideEntity() }

        // Register user for lendings
        Database {
            LendingUserEntity.new {
                userSub = user.sub
                phoneNumber = "123456789"
                sports = listOf(Sports.HIKING)
            }
        }

        // Add insurance for the user
        Database {
            UserInsuranceEntity.new {
                userSub = user
                validFrom = LocalDate.of(2025, 1, 1)
                validTo = LocalDate.of(2025, 12, 31)
                insuranceCompany = "Insurance Co"
                policyNumber = "POL123456"
            }
        }

        today = { LocalDate.of(2025, 10, 8) }

        // Single item
        val location = client.submitForm(
            "/inventory/lendings",
            parameters {
                append("from", "2025-10-10")
                append("to", "2025-10-11")
                append("items", exampleItemId.toString())
                append("notes", "These are some notes")
            }
        ).run {
            assertStatusCode(HttpStatusCode.Created)
            val location = headers[HttpHeaders.Location]
            assertNotNull(location)
            assertTrue("Unexpected format for location: $location") {
                location.matches("/inventory/lendings/[0-9a-f-]+".toRegex())
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
                assertEquals(LocalDate.of(2025, 10, 10), lending.from.toJavaLocalDate())
                assertEquals(LocalDate.of(2025, 10, 11), lending.to.toJavaLocalDate())
                assertEquals("These are some notes", lending.notes)
                assertEquals(1, lending.items.size)
                assertEquals(item.id.value, lending.items[0].id.toJavaUuid())
            }
        }
    }


    @Test
    fun test_allocate_lending_correct() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            getOrCreateItem()

            // The reference user must exist in the database
            val user = Database { FakeUser.provideEntity() }

            // Register user for lendings
            Database {
                LendingUserEntity.new {
                    userSub = user.sub
                    phoneNumber = "123456789"
                    sports = listOf(Sports.HIKING)
                }
            }

            // Add insurance for the user
            Database {
                UserInsuranceEntity.new {
                    userSub = user
                    validFrom = LocalDate.of(2025, 1, 1)
                    validTo = LocalDate.of(2025, 12, 31)
                    insuranceCompany = "Insurance Co"
                    policyNumber = "POL123456"
                }
            }
        },
        finally = { today = { LocalDate.now() } },
    ) {
        today = { LocalDate.of(2025, 10, 8) }

        client.get(
            "/inventory/types/$exampleItemTypeId/allocate?from=2025-10-10&to=2025-10-11&amount=1",
        ).run {
            assertStatusCode(HttpStatusCode.OK)
        }
    }


    @Test
    fun test_list_lending_notLoggedIn() = ProvidedRouteTests.test_notLoggedIn("/inventory/lendings")

    @Test
    fun test_list_lending_notAdmin() = runApplicationTest(
        shouldLogIn = LoginType.USER,
        databaseInitBlock = {
            getOrCreateItem()

            // Existing lending from 2025-10-10 to 2025-10-15
            val user = FakeUser.provideEntity()
            val admin = FakeAdminUser.provideEntity()

            val userLending = LendingEntity.new {
                this.userSub = user
                this.from = LocalDate.of(2025, 10, 10)
                this.to = LocalDate.of(2025, 10, 15)
                this.notes = "Existing lending"
            }.also { lendingEntity ->
                LendingItems.insert {
                    it[item] = exampleItemId
                    it[lending] = lendingEntity.id
                }
            }
            val adminLending = LendingEntity.new {
                this.userSub = admin
                this.from = LocalDate.of(2025, 11, 10)
                this.to = LocalDate.of(2025, 11, 15)
                this.notes = "Admin lending"
            }.also { lendingEntity ->
                LendingItems.insert {
                    it[item] = exampleItemId
                    it[lending] = lendingEntity.id
                }
            }
            userLending to adminLending
        }
    ) { context ->
        val lending = context.dibResult
        assertNotNull(lending)
        val (userLending, adminLending) = lending

        client.get("/inventory/lendings").apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(Lending.serializer().list()) { lendings ->
                assertEquals(1, lendings.size)
                val lending = lendings[0]
                assertEquals(userLending.id.value, lending.id.toJavaUuid())
                assertEquals(FakeUser.SUB, lending.userSub)
            }
        }
    }

    @Test
    fun test_list_lending_admin() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            getOrCreateItem()

            // Existing lending from 2025-10-10 to 2025-10-15
            val user = FakeUser.provideEntity()
            val admin = FakeAdminUser.provideEntity()

            val userLending = LendingEntity.new {
                this.userSub = user
                this.from = LocalDate.of(2025, 10, 10)
                this.to = LocalDate.of(2025, 10, 15)
                this.notes = "Existing lending"
            }.also { lendingEntity ->
                LendingItems.insert {
                    it[item] = exampleItemId
                    it[lending] = lendingEntity.id
                }
            }
            val adminLending = LendingEntity.new {
                this.userSub = admin
                this.from = LocalDate.of(2025, 11, 10)
                this.to = LocalDate.of(2025, 11, 15)
                this.notes = "Admin lending"
            }.also { lendingEntity ->
                LendingItems.insert {
                    it[item] = exampleItemId
                    it[lending] = lendingEntity.id
                }
            }
            userLending to adminLending
        }
    ) { context ->
        val lending = context.dibResult
        assertNotNull(lending)
        val (userLending, adminLending) = lending

        client.get("/inventory/lendings").apply {
            assertStatusCode(HttpStatusCode.OK)
            assertBody(Lending.serializer().list()) { lendings ->
                assertEquals(2, lendings.size)
                val lending1 = lendings.find { it.id.toJavaUuid() == userLending.id.value }
                assertNotNull(lending1)
                assertEquals(FakeUser.SUB, lending1.userSub)
                val lending2 = lendings.find { it.id.toJavaUuid() == adminLending.id.value }
                assertNotNull(lending2)
                assertEquals(FakeAdminUser.SUB, lending2.userSub)
            }
        }
    }

    @Test
    // tests deleting an item that is already referenced on a lending
    fun test_delete_item_with_ending() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            getOrCreateItem()

            val user = FakeUser.provideEntity()

            LendingEntity.new {
                this.userSub = user
                this.from = LocalDate.of(2025, 10, 10)
                this.to = LocalDate.of(2025, 10, 15)
                this.notes = "Existing lending"
            }.also { lendingEntity ->
                LendingItems.insert {
                    it[item] = exampleItemId
                    it[lending] = lendingEntity.id
                }
            }
        }
    ) {
        // try deleting exampleItemId
        client.delete("/inventory/items/$exampleItemId").apply {
            assertError(Error.EntityDeleteReferencesExist())
        }
    }

    @Test
    fun test_pickup_lending_without_dismissing() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            getOrCreateItem()

            val item2Id = "b27a6569-84fa-443f-9ce5-4b24279f0471".toUUID()
            getOrCreateItem(id = item2Id)

            val user = FakeUser.provideEntity()

            LendingEntity.new {
                this.userSub = user
                this.from = LocalDate.of(2025, 10, 10)
                this.to = LocalDate.of(2025, 10, 15)
                this.confirmed = true
            }.also { lendingEntity ->
                LendingItems.insert {
                    it[item] = exampleItemId
                    it[lending] = lendingEntity.id
                }
                LendingItems.insert {
                    it[item] = item2Id
                    it[lending] = lendingEntity.id
                }
            }
        }
    ) { context ->
        val entity = context.dibResult!!
        client.post("inventory/lendings/${entity.id.value}/pickup").apply {
            assertStatusCode(HttpStatusCode.NoContent)
        }
    }

    @Test
    fun test_pickup_lending_with_item_dismiss() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            getOrCreateItem()

            val item2Id = "b27a6569-84fa-443f-9ce5-4b24279f0471".toUUID()
            getOrCreateItem(id = item2Id)

            val user = FakeUser.provideEntity()

            LendingEntity.new {
                this.userSub = user
                this.from = LocalDate.of(2025, 10, 10)
                this.to = LocalDate.of(2025, 10, 15)
                this.confirmed = true
            }.also { lendingEntity ->
                LendingItems.insert {
                    it[item] = exampleItemId
                    it[lending] = lendingEntity.id
                }
                LendingItems.insert {
                    it[item] = item2Id
                    it[lending] = lendingEntity.id
                }
            }
        }
    ) { context ->
        val entity = context.dibResult!!
        client.submitForm(
            "inventory/lendings/${entity.id.value}/pickup",
            parameters {
                append("dismiss_items", exampleItemId.toString())
            }
        ).apply {
            assertStatusCode(HttpStatusCode.NoContent)
            val dismissedItems = headers["CEA-Dismissed-Items"]
            assertNotNull(dismissedItems)
            val dismissedItemIds = dismissedItems.split(',').mapNotNull { it.toUUIDOrNull() }
            assertEquals(1, dismissedItemIds.size)
            assertEquals(exampleItemId, dismissedItemIds[0])
        }

        // Make sure the item was removed from the lending
        Database {
            val lendingEntity = LendingEntity[entity.id.value]
            val items = lendingEntity.items.toList()
            assertEquals(1, items.size)
            assertEquals("b27a6569-84fa-443f-9ce5-4b24279f0471".toUUID(), items[0].id.value)
        }
    }

    @Test
    fun test_return_lending_all_items() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            getOrCreateItem()

            val item2Id = "b27a6569-84fa-443f-9ce5-4b24279f0471".toUUID()
            getOrCreateItem(id = item2Id)

            val user = FakeUser.provideEntity()

            LendingEntity.new {
                this.userSub = user
                this.from = LocalDate.of(2025, 10, 10)
                this.to = LocalDate.of(2025, 10, 15)
                this.confirmed = true
                this.taken = true
                this.givenBy = FakeAdminUser.provideEntity().sub
                this.givenAt = LocalDate.of(2025, 10, 9).atStartOfDay().toInstant(ZoneOffset.UTC)
            }.also { lendingEntity ->
                LendingItems.insert {
                    it[item] = exampleItemId
                    it[lending] = lendingEntity.id
                }
                LendingItems.insert {
                    it[item] = item2Id
                    it[lending] = lendingEntity.id
                }
            }
        }
    ) { context ->
        val entity = context.dibResult!!
        client.post(
            "inventory/lendings/${entity.id.value}/return",
        ) {
            contentType(ContentType.Application.Json)
            setBody(
                json.encodeToString(
                    ReturnLendingRequest.serializer(),
                    ReturnLendingRequest(
                        returnedItems = listOf(
                            ReturnLendingRequest.ReturnedItem(exampleItemId.toKotlinUuid(), "All good"),
                            ReturnLendingRequest.ReturnedItem("b27a6569-84fa-443f-9ce5-4b24279f0471".toUuid())
                        )
                    )
                )
            )
        }.apply {
            assertStatusCode(HttpStatusCode.NoContent)
        }

        // Make sure the lending is marked as returned
        Database {
            val lendingEntity = LendingEntity[entity.id.value]
            assertTrue(lendingEntity.returned)
        }

        // Make sure all items are marked as received
        Database {
            val receivedItems = ReceivedItemEntity.find { ReceivedItems.lending eq entity.id.value }.toList()
            assertEquals(2, receivedItems.size)
            receivedItems[0].let { receivedItem ->
                assertEquals(exampleItemId, receivedItem.item.id.value)
                assertEquals(FakeAdminUser.SUB, receivedItem.receivedBy.sub.value)
                assertEquals("All good", receivedItem.notes)
            }
            receivedItems[1].let { receivedItem ->
                assertEquals("b27a6569-84fa-443f-9ce5-4b24279f0471".toUUID(), receivedItem.item.id.value)
                assertEquals(FakeAdminUser.SUB, receivedItem.receivedBy.sub.value)
                assertEquals(null, receivedItem.notes)
            }
        }
    }

    @Test
    fun test_return_lending_partial_items() = runApplicationTest(
        shouldLogIn = LoginType.ADMIN,
        databaseInitBlock = {
            getOrCreateItem()

            val item2Id = "b27a6569-84fa-443f-9ce5-4b24279f0471".toUUID()
            getOrCreateItem(id = item2Id)

            val user = FakeUser.provideEntity()

            LendingEntity.new {
                this.userSub = user
                this.from = LocalDate.of(2025, 10, 10)
                this.to = LocalDate.of(2025, 10, 15)
                this.confirmed = true
                this.taken = true
                this.givenBy = FakeAdminUser.provideEntity().sub
                this.givenAt = LocalDate.of(2025, 10, 9).atStartOfDay().toInstant(ZoneOffset.UTC)
            }.also { lendingEntity ->
                LendingItems.insert {
                    it[item] = exampleItemId
                    it[lending] = lendingEntity.id
                }
                LendingItems.insert {
                    it[item] = item2Id
                    it[lending] = lendingEntity.id
                }
            }
        }
    ) { context ->
        val entity = context.dibResult!!
        client.post(
            "inventory/lendings/${entity.id.value}/return",
        ) {
            contentType(ContentType.Application.Json)
            setBody(
                json.encodeToString(
                    ReturnLendingRequest.serializer(),
                    ReturnLendingRequest(
                        returnedItems = listOf(
                            ReturnLendingRequest.ReturnedItem(exampleItemId.toKotlinUuid(), "All good"),
                        )
                    )
                )
            )
        }.apply {
            assertStatusCode(HttpStatusCode.Accepted)
            headers["CEA-Missing-Items"]?.let { remainingItemsHeader ->
                val remainingItemIds = remainingItemsHeader.split(',').mapNotNull { it.toUUIDOrNull() }
                assertEquals(1, remainingItemIds.size)
                assertEquals("b27a6569-84fa-443f-9ce5-4b24279f0471".toUUID(), remainingItemIds[0])
            } ?: throw AssertionError("Missing CEA-Missing-Items header in response")
        }

        // Make sure the lending is not marked as returned
        Database {
            val lendingEntity = LendingEntity[entity.id.value]
            assertFalse(lendingEntity.returned)
        }

        // Make sure the item is marked as received
        Database {
            val receivedItems = ReceivedItemEntity.find { ReceivedItems.lending eq entity.id.value }.toList()
            assertEquals(1, receivedItems.size)
            receivedItems[0].let { receivedItem ->
                assertEquals(exampleItemId, receivedItem.item.id.value)
                assertEquals(FakeAdminUser.SUB, receivedItem.receivedBy.sub.value)
                assertEquals("All good", receivedItem.notes)
            }
        }
    }
}
