package org.centrexcursionistalcoi.app.database.entity

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.toKotlinLocalDate
import org.centrexcursionistalcoi.app.assertJsonEquals
import org.centrexcursionistalcoi.app.data.*
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.table.LendingItems
import org.centrexcursionistalcoi.app.database.utils.encodeEntityToString
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.test.FakeAdminUser
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.utils.toUUID
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant
import java.time.LocalDate
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.toKotlinInstant
import kotlin.uuid.toKotlinUuid

class TestLendings {
    @AfterTest
    fun tearDown() = runTest {
        Database.clear()
    }

    @Test
    fun test_create() = runTest {
        Database.initForTests()

        val inventoryItemType = Database {
            InventoryItemTypeEntity.new {
                displayName = "Type"
            }
        }
        val inventoryItem = Database {
            InventoryItemEntity.new {
                type = inventoryItemType
            }
        }

        val lending = Database {
            val lending = LendingEntity.new {
                userSub = transaction { FakeUser.provideEntity() }
                from = LocalDate.of(2025, 10, 8)
                to = LocalDate.of(2025, 10, 9)
            }
            LendingItems.insert {
                it[LendingItems.lending] = lending.id
                it[LendingItems.item] = inventoryItem.id
            }
            lending
        }
        val items = Database { lending.items.toList() }
        assertEquals(1, items.size)

        // Verify that the same item cannot be added twice to the same lending
        assertFailsWith<ExposedSQLException> {
            Database {
                LendingItems.insert {
                    it[LendingItems.lending] = lending.id
                    it[LendingItems.item] = inventoryItem.id
                }
            }
        }
    }

    @Test
    fun test_create_endNotBeforeStart() = runTest {
        Database.initForTests()

        assertFailsWith<ExposedSQLException> {
            Database {
                LendingEntity.new {
                    userSub = transaction { FakeUser.provideEntity() }
                    from = LocalDate.of(2025, 10, 10)
                    to = LocalDate.of(2025, 10, 9)
                }
            }
        }
    }

    @Test
    fun `test entity serializes the same as data class`() = runTest {
        Database.initForTests()

        val id = "315cedda-a219-426d-8acc-ebeb7c70b9f7".toUUID()
        val itemId = "3582407a-6c08-44ce-abf5-6a8545c48516".toUUID()
        val itemTypeId = "2c3c5f3d-5c5e-4916-988c-225b57f91cfa".toUUID()
        val receivedItemId = "786a1c86-6e7d-4cdc-bebf-d1540f67029b".toUUID()
        val memoryPdfFileId = "bd84fb5c-9356-4abe-a8e5-0aea77b7b7cb".toUUID()
        val memoryAttachmentFileId = "79f71564-2b24-4612-911a-913ef4e7d23f".toUUID()

        val instant = Instant.ofEpochSecond(1759917121)

        val department = Database {
            DepartmentEntity.new {
                displayName = "Department"
            }
        }
        val memoryPdfFileEntity = Database {
            FileEntity.new(memoryPdfFileId) {
                name = "memory.pdf"
                type = "application/pdf"
                bytes = byteArrayOf(1, 2, 3, 4)
            }
        }
        val entity = Database {
            LendingEntity.new(id) {
                timestamp = instant
                userSub = transaction { FakeUser.provideEntity() }
                from = LocalDate.of(2025, 10, 8)
                to = LocalDate.of(2025, 10, 9)
                confirmed = true
                taken = true
                givenBy = transaction { FakeAdminUser.provideEntity().id }
                givenAt = instant
                returned = true
                notes = "notes"
                memorySubmitted = true
                memorySubmittedAt = instant
                memory = LendingMemory(
                    place = "Place",
                    members = listOf(transaction { FakeUser.provideMemberEntity() }.memberNumber),
                    externalUsers = "John Doe",
                    text = "Lending memory text",
                    files = listOf(memoryAttachmentFileId.toKotlinUuid()),
                    department = department.id.value.toKotlinUuid(),
                    sport = Sports.ORIENTEERING,
                )
                memoryPdf = memoryPdfFileEntity
                memoryReviewed = true
            }
        }
        // Create and link inventory item
        val itemEntity = Database {
            InventoryItemEntity.new(itemId) {
                type = transaction {
                    InventoryItemTypeEntity.new(itemTypeId) {
                        displayName = "Type"
                        variation = "variation"
                        nfcId = byteArrayOf(0, 1, 2, 3)
                    }
                }
            }
        }.also { item ->
            Database {
                LendingItems.insert {
                    it[LendingItems.lending] = entity.id
                    it[LendingItems.item] = item.id
                }
            }
        }
        // Create and link received item
        Database {
            ReceivedItemEntity.new(receivedItemId) {
                lending = entity
                item = itemEntity
                notes = "Good"
                receivedAt = instant
                receivedBy = transaction { FakeAdminUser.provideEntity() }
            }
        }
        // Upload memory attachment
        Database {
            FileEntity.new(memoryAttachmentFileId) {
                name = "attachment.pdf"
                type = "application/pdf"
                bytes = byteArrayOf(1, 2, 3, 4)
            }
        }

        val instance = Lending(
            id = id.toKotlinUuid(),
            userSub = FakeUser.SUB,
            timestamp = instant.toKotlinInstant(),
            from = LocalDate.of(2025, 10, 8).toKotlinLocalDate(),
            to = LocalDate.of(2025, 10, 9).toKotlinLocalDate(),
            confirmed = true,
            taken = true,
            givenBy = FakeAdminUser.SUB,
            givenAt = instant.toKotlinInstant(),
            returned = true,
            receivedItems = listOf(
                ReceivedItem(
                    id = receivedItemId.toKotlinUuid(),
                    lendingId = id.toKotlinUuid(),
                    itemId = itemId.toKotlinUuid(),
                    notes = "Good",
                    receivedAt = instant.toKotlinInstant(),
                    receivedBy = FakeAdminUser.SUB
                )
            ),
            notes = "notes",
            memorySubmitted = true,
            memorySubmittedAt = instant.toKotlinInstant(),
            memory = LendingMemory(
                place = "Place",
                members = listOf(transaction { FakeUser.provideMemberEntity() }.memberNumber),
                externalUsers = "John Doe",
                text = "Lending memory text",
                files = listOf(memoryAttachmentFileId.toKotlinUuid()),
                department = department.id.value.toKotlinUuid(),
                sport = Sports.ORIENTEERING,
            ),
            memoryPdf = memoryPdfFileId.toKotlinUuid(),
            memoryReviewed = true,
            items = listOf(
                InventoryItem(itemId.toKotlinUuid(), "variation", itemTypeId.toKotlinUuid(), byteArrayOf(0, 1, 2, 3))
            ),
        )

        assertJsonEquals(
            json.encodeToString(Lending.serializer(), instance),
            json.encodeEntityToString(entity),
            ignoreKeys = setOf("files")
        )
    }
}
