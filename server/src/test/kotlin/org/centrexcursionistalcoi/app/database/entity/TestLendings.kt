package org.centrexcursionistalcoi.app.database.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.ApplicationTestBase.FakeUser
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.Database.TEST_URL
import org.centrexcursionistalcoi.app.database.table.LendingItems
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class TestLendings {
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
            }
            LendingItems.insert {
                it[LendingItems.lending] = lending.id
                it[LendingItems.item] = inventoryItem.id
            }
            lending
        }
        assertEquals(1, Database { lending.items.count() })

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
}
