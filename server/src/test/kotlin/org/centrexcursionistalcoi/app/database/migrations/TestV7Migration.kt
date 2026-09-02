package org.centrexcursionistalcoi.app.database.migrations

import org.centrexcursionistalcoi.app.assertTrue
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.PostgresTestBase
import org.centrexcursionistalcoi.app.database.entity.LendingEntity
import org.centrexcursionistalcoi.app.security.AES
import org.centrexcursionistalcoi.app.test.FakeUser
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests the migration that adds the required `from`/`to` date range to `memories` (stored as
 * `fromInstant`/`fromZone` and `toInstant`/`toZone`): backfilled from the linked lending's own `from`/`to` for
 * lending memories, and from the memory's own `createdAt` for standalone ones.
 */
class TestV7Migration : PostgresTestBase() {
    @Test
    fun test() {
        AES.initForTests()

        // Create the current schema (already includes the new NOT NULL `fromInstant`/`fromZone`/`toInstant`/
        // `toZone` columns on `memories`).
        Database.init()

        val user = Database { transaction { FakeUser.provideEntity() } }
        val lending = Database {
            LendingEntity.new {
                userSub = user
                from = LocalDate.of(2025, 10, 8)
                to = LocalDate.of(2025, 10, 9)
                returned = true
            }
        }

        // Simulate a pre-V7 database: the memories date-range columns don't exist yet.
        Database.exec(
            """
                ALTER TABLE memories DROP COLUMN "fromInstant";
                ALTER TABLE memories DROP COLUMN "fromZone";
                ALTER TABLE memories DROP COLUMN "toInstant";
                ALTER TABLE memories DROP COLUMN "toZone";
            """.trimIndent()
        ).assertTrue()

        val lendingMemoryId = UUID.randomUUID()
        Database.exec(
            """INSERT INTO memories (id, "text", "submittedBy", lending) VALUES (?::uuid, ?, ?, ?::uuid)""",
            arrayOf(lendingMemoryId.toString(), "Lending memory", FakeUser.SUB, lending.id.value.toString())
        ).assertTrue()

        val standaloneMemoryId = UUID.randomUUID()
        Database.exec(
            """INSERT INTO memories (id, "text", "submittedBy") VALUES (?::uuid, ?, ?)""",
            arrayOf(standaloneMemoryId.toString(), "Standalone memory", FakeUser.SUB)
        ).assertTrue()

        // Migrate
        Database { V7.migrate() }

        val zoneId = ZoneId.systemDefault().id

        // Lending memory: from/to backfilled from the lending's own date range (start of day / end of day, in the
        // server's local time zone), with both zone columns set to that same zone.
        Database.execQuery(
            """SELECT "fromInstant", "fromZone", "toInstant", "toZone" FROM memories WHERE id = '$lendingMemoryId'"""
        ).let { rs ->
            assertTrue(rs.next())
            val expectedFrom = lending.from.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val expectedTo = lending.to.atTime(LocalTime.of(23, 59, 59)).atZone(ZoneId.systemDefault()).toInstant()
            assertEquals(expectedFrom, rs.getTimestamp("fromInstant").toInstant())
            assertEquals(zoneId, rs.getString("fromZone"))
            assertEquals(expectedTo, rs.getTimestamp("toInstant").toInstant())
            assertEquals(zoneId, rs.getString("toZone"))
        }

        // Standalone memory: from/to backfilled from its own createdAt, also with the server's local zone.
        Database.execQuery(
            """SELECT "fromInstant", "fromZone", "toInstant", "toZone", "createdAt" FROM memories WHERE id = '$standaloneMemoryId'"""
        ).let { rs ->
            assertTrue(rs.next())
            val createdAt = rs.getTimestamp("createdAt").toInstant()
            assertEquals(createdAt, rs.getTimestamp("fromInstant").toInstant())
            assertEquals(zoneId, rs.getString("fromZone"))
            assertEquals(createdAt, rs.getTimestamp("toInstant").toInstant())
            assertEquals(zoneId, rs.getString("toZone"))
        }

        // All four columns are now NOT NULL.
        Database.execQuery(
            """
                SELECT column_name FROM information_schema.columns
                WHERE table_name = 'memories'
                    AND column_name IN ('fromInstant', 'fromZone', 'toInstant', 'toZone')
                    AND is_nullable = 'NO'
            """.trimIndent()
        ).let { rs ->
            val notNullColumns = generateSequence { if (rs.next()) rs.getString("column_name") else null }.toSet()
            assertEquals(setOf("fromInstant", "fromZone", "toInstant", "toZone"), notNullColumns)
        }
    }
}
