package org.centrexcursionistalcoi.app.storage

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.centrexcursionistalcoi.app.database.Database
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the local (SQLDelight) database migration defined in `migrations/15.sqm`, which moves memories out of the
 * `Lendings.memory`/`memoryPdf` embedded columns into their own `Memories` table -- mirroring the server-side move
 * from an embedded JSON blob to a normalized `memories` table with a foreign key back to the lending.
 *
 * Note: this is database version 15 -> 16 (`Database.Schema.version` is the number of migration files plus one),
 * since `15.sqm` is the migration that runs when upgrading *from* version 15.
 *
 * `JdbcSqliteDriver` executes everything synchronously (it always returns [QueryResult.Value]), so this test uses
 * [Database.Schema.synchronous] and reads `.value` directly rather than going through the suspending `await*`
 * helpers -- those unconditionally wrap query mappers in [QueryResult.AsyncValue], which defers their execution
 * past the point where the underlying JDBC cursor is already closed for a driver that never actually suspends.
 */
class TestMigration15 {

    private fun SqlDriver.exec(sql: String) {
        execute(null, sql, 0).value
    }

    private fun SqlDriver.rowCount(table: String): Long = executeQuery(
        null,
        "SELECT COUNT(*) FROM $table",
        { cursor ->
            cursor.next().value
            QueryResult.Value(cursor.getLong(0)!!)
        },
        0,
    ).value

    private fun SqlDriver.columnNames(table: String): List<String> = executeQuery(
        null,
        "PRAGMA table_info($table)",
        { cursor ->
            val names = mutableListOf<String>()
            while (cursor.next().value) {
                names += cursor.getString(1)!!
            }
            QueryResult.Value(names)
        },
        0,
    ).value

    @Test
    fun test_migrate_15_to_16() {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            val schema = Database.Schema.synchronous()

            // SQLDelight's `.sqm` migrations only encode deltas on top of whatever the schema looked like when the
            // app first shipped (version 0) -- they can't be replayed from an empty database on their own. So the
            // "version 15" state is built here from the current (latest) schema instead: `Lendings`/`LendingItems`/
            // `ReceivedItems` are untouched by any migration other than 15.sqm, so creating the full current schema
            // and then patching just `Lendings`/`Memories` back to their pre-15.sqm shape reproduces version 15
            // exactly for the tables this migration actually cares about.
            schema.create(driver).value
            driver.exec("DROP TABLE Memories")
            driver.exec("ALTER TABLE Lendings ADD COLUMN memory TEXT")
            driver.exec("ALTER TABLE Lendings ADD COLUMN memoryPdf TEXT")

            // Sanity check we're really at the old (pre-migration) shape.
            val oldLendingsColumns = driver.columnNames("Lendings")
            assertTrue("memory" in oldLendingsColumns, "Expected the old `memory` column before migrating")
            assertTrue("memoryPdf" in oldLendingsColumns, "Expected the old `memoryPdf` column before migrating")

            // Insert a lending using the old (v15) shape, plus dependent rows in LendingItems/ReceivedItems.
            driver.exec(
                """
                        INSERT INTO Lendings (id, userSub, timestamp, fromDate, toDate, confirmed, taken, givenBy, givenAt, returned, memorySubmitted, memorySubmittedAt, memory, memoryPdf, memoryReviewed, notes)
                        VALUES ('lending-1', 'user-1', 0, '2025-10-08', '2025-10-09', 0, 0, NULL, NULL, 1, 1, 0, NULL, NULL, 0, NULL)
                    """.trimIndent()
            )
            driver.exec("INSERT INTO LendingItems (lendingId, itemId) VALUES ('lending-1', 'item-1')")
            driver.exec("INSERT INTO ReceivedItems (id, lending, item, notes, receivedBy, receivedAt) VALUES ('received-1', 'lending-1', 'item-1', NULL, 'user-1', 0)")

            assertEquals(1L, driver.rowCount("Lendings"))
            assertEquals(1L, driver.rowCount("LendingItems"))
            assertEquals(1L, driver.rowCount("ReceivedItems"))

            // Run the migration under test.
            schema.migrate(driver, 15, 16).value

            // Lendings, LendingItems and ReceivedItems were cleared -- a full sync repopulates them afterward
            // (Lendings is dropped and recreated to remove the memory/memoryPdf columns; its dependents are
            // cleared first since SQLite can't cheaply drop a column any other way).
            assertEquals(0L, driver.rowCount("Lendings"))
            assertEquals(0L, driver.rowCount("LendingItems"))
            assertEquals(0L, driver.rowCount("ReceivedItems"))

            // Lendings no longer has the memory/memoryPdf columns.
            val newLendingsColumns = driver.columnNames("Lendings")
            assertFalse("memory" in newLendingsColumns, "memory column should have been dropped from Lendings")
            assertFalse("memoryPdf" in newLendingsColumns, "memoryPdf column should have been dropped from Lendings")

            // Memories now exists, with the expected columns, and is queryable (empty, since it's brand new).
            val memoriesColumns = driver.columnNames("Memories")
            assertEquals(
                setOf("id", "place", "members", "externalUsers", "text", "sport", "department", "attachments", "submittedBy", "pdf", "lending"),
                memoriesColumns.toSet(),
            )
            assertEquals(0L, driver.rowCount("Memories"))
        }
    }
}
