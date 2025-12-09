package org.centrexcursionistalcoi.app.utils

import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.InventoryItemEntity
import org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity
import org.centrexcursionistalcoi.app.database.entity.LendingEntity
import org.centrexcursionistalcoi.app.database.table.LendingItems
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.utils.LendingUtils.conflictsWith
import org.jetbrains.exposed.v1.jdbc.insert
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LendingUtilsTest {
    @Test
    fun test_conflictsWith() = runTest {
        Database.initForTests()

        val user = Database { FakeUser.provideEntity() }

        val type = Database {
            InventoryItemTypeEntity.new {
                displayName = "Type"
            }
        }

        val item1 = Database {
            InventoryItemEntity.new {
                this.type = type
                this.variation = "Item 1"
            }
        }
        val item2 = Database {
            InventoryItemEntity.new {
                this.type = type
                this.variation = "Item 2"
            }
        }
        val item3 = Database {
            InventoryItemEntity.new {
                this.type = type
                this.variation = "Item 3"
            }
        }

        Database {
            // Create a lending from 2025-10-01 to 2025-10-03 with item1 and item2
            LendingEntity.new {
                userSub = user
                from = LocalDate.of(2025, 10, 1)
                to = LocalDate.of(2025, 10, 3)
            }.also { entity ->
                LendingItems.insert {
                    it[lending] = entity.id
                    it[item] = item1.id
                }
                LendingItems.insert {
                    it[lending] = entity.id
                    it[item] = item2.id
                }
            }
            // Create a lending from 2025-1-1 to 2025-12-31 with all items, but completed
            LendingEntity.new {
                userSub = user
                from = LocalDate.of(2025, 1, 1)
                to = LocalDate.of(2025, 12, 31)
                // set all fields to complete
                confirmed = true
                taken = true
                returned = true
            }.also { entity ->
                LendingItems.insert {
                    it[lending] = entity.id
                    it[item] = item1.id
                }
                LendingItems.insert {
                    it[lending] = entity.id
                    it[item] = item2.id
                }
                LendingItems.insert {
                    it[lending] = entity.id
                    it[item] = item3.id
                }
            }
        }

        val lendings = Database { LendingEntity.all() }
        // Out of range is fine
        assertFalse("Before") {
            Database {
                lendings.conflictsWith(
                    from = LocalDate.of(2025, 5, 1),
                    to = LocalDate.of(2025, 5, 2),
                    items = listOf(item1, item2, item3)
                )
            }
        }
        assertFalse("After") {
            Database {
                lendings.conflictsWith(
                    from = LocalDate.of(2025, 11, 1),
                    to = LocalDate.of(2025, 11, 2),
                    items = listOf(item1, item2, item3)
                )
            }
        }
        // Overlapping dates with different items is fine
        assertFalse("Start overlap different items") {
            Database {
                lendings.conflictsWith(
                    from = LocalDate.of(2025, 10, 1),
                    to = LocalDate.of(2025, 10, 2),
                    items = listOf(item3)
                )
            }
        }
        // Overlapping dates with same items is not fine
        assertTrue("Start overlap same items") {
            Database {
                lendings.conflictsWith(
                    from = LocalDate.of(2025, 10, 1),
                    to = LocalDate.of(2025, 10, 1),
                    items = listOf(item1, item3)
                )
            }
        }
        assertTrue("Enclosed same items") {
            Database {
                lendings.conflictsWith(
                    from = LocalDate.of(2025, 10, 1),
                    to = LocalDate.of(2025, 10, 2),
                    items = listOf(item1, item3)
                )
            }
        }
        assertTrue("End overlap same items") {
            Database {
                lendings.conflictsWith(
                    from = LocalDate.of(2025, 10, 2),
                    to = LocalDate.of(2025, 10, 3),
                    items = listOf(item1, item3)
                )
            }
        }
        assertTrue("End overlap same items inclusive") {
            Database {
                lendings.conflictsWith(
                    from = LocalDate.of(2025, 10, 3),
                    to = LocalDate.of(2025, 10, 3),
                    items = listOf(item1, item3)
                )
            }
        }
        assertTrue("Enclosing same items") {
            Database {
                lendings.conflictsWith(
                    from = LocalDate.of(2025, 9, 25),
                    to = LocalDate.of(2025, 10, 1),
                    items = listOf(item1, item3)
                )
            }
        }
        assertTrue("Enclosing same items inclusive") {
            Database {
                lendings.conflictsWith(
                    from = LocalDate.of(2025, 9, 25),
                    to = LocalDate.of(2025, 10, 3),
                    items = listOf(item1, item3)
                )
            }
        }
        assertTrue("Enclosing same items after") {
            Database {
                lendings.conflictsWith(
                    from = LocalDate.of(2025, 10, 1),
                    to = LocalDate.of(2025, 10, 25),
                    items = listOf(item1, item3)
                )
            }
        }
        assertTrue("Enclosing same items inclusive after") {
            Database {
                lendings.conflictsWith(
                    from = LocalDate.of(2025, 10, 3),
                    to = LocalDate.of(2025, 10, 25),
                    items = listOf(item1, item3)
                )
            }
        }
    }
}
