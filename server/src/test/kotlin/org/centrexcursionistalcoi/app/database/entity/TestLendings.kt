package org.centrexcursionistalcoi.app.database.entity

import java.time.Instant
import java.time.LocalDate
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.toKotlinInstant
import kotlin.uuid.toKotlinUuid
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.toKotlinLocalDate
import org.centrexcursionistalcoi.app.assertJsonEquals
import org.centrexcursionistalcoi.app.data.InventoryItem
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.Database.TEST_URL
import org.centrexcursionistalcoi.app.database.table.LendingItems
import org.centrexcursionistalcoi.app.database.utils.encodeEntityToString
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.utils.toUUID
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class TestLendings {
    @AfterTest
    fun tearDown() = runTest {
        Database.clear()
    }

    @Test
    fun test_create() = runTest {
        Database.init(TEST_URL)

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
        Database.init(TEST_URL)

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
        Database.init(TEST_URL)

        val id = "315cedda-a219-426d-8acc-ebeb7c70b9f7".toUUID()
        val itemId = "3582407a-6c08-44ce-abf5-6a8545c48516".toUUID()
        val itemTypeId = "2c3c5f3d-5c5e-4916-988c-225b57f91cfa".toUUID()
        val instant = Instant.ofEpochSecond(1759917121)
        val entity = Database {
            LendingEntity.new(id) {
                timestamp = instant
                userSub = transaction { FakeUser.provideEntity() }
                from = LocalDate.of(2025, 10, 8)
                to = LocalDate.of(2025, 10, 9)
            }
        }
        val item = Database {
            InventoryItemEntity.new(itemId) {
                type = transaction {
                    InventoryItemTypeEntity.new(itemTypeId) {
                        displayName = "Type"
                    }
                }
            }
        }
        Database {
            LendingItems.insert {
                it[LendingItems.lending] = entity.id
                it[LendingItems.item] = item.id
            }
        }

        val instance = Lending(
            id = id.toKotlinUuid(),
            userSub = FakeUser.SUB,
            timestamp = instant.toKotlinInstant(),
            confirmed = false,
            taken = false,
            givenBy = null,
            givenAt = null,
            returned = false,
            receivedBy = null,
            receivedAt = null,
            from = LocalDate.of(2025, 10, 8).toKotlinLocalDate(),
            to = LocalDate.of(2025, 10, 9).toKotlinLocalDate(),
            notes = null,
            items = listOf(
                InventoryItem(itemId.toKotlinUuid(), null, itemTypeId.toKotlinUuid())
            ),
        )

        assertJsonEquals(
            json.encodeEntityToString(entity),
            json.encodeToString(Lending.serializer(), instance)
        )
    }
}
