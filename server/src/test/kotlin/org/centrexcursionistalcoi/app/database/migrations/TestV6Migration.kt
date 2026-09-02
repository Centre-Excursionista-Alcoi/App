package org.centrexcursionistalcoi.app.database.migrations

import org.centrexcursionistalcoi.app.assertTrue
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.PostgresTestBase
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.database.entity.LendingEntity
import org.centrexcursionistalcoi.app.security.AES
import org.centrexcursionistalcoi.app.test.FakeUser
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the migration that moves memories from a JSON blob (plus a separate `memoryPdf` reference) embedded in
 * `lendings`, into their own normalized tables (`memories`, `memories_members`, `memories_files`).
 */
class TestV6Migration : PostgresTestBase() {
    @Test
    fun test() {
        AES.initForTests()

        // Create the current schema (this already includes the new `memories`, `memories_members` and
        // `memories_files` tables, and the `lendings` table without the legacy `memory`/`memoryPdf` columns).
        Database.init()

        val department = Database {
            DepartmentEntity.new { displayName = "Department" }
        }
        val pdfFile = Database {
            FileEntity.new {
                name = "memory.pdf"
                type = "application/pdf"
                bytes = byteArrayOf(1, 2, 3, 4)
            }
        }
        val attachmentFile = Database {
            FileEntity.new {
                name = "attachment.jpg"
                type = "image/jpeg"
                bytes = byteArrayOf(5, 6, 7, 8)
            }
        }
        val member = Database { transaction { FakeUser.provideMemberEntity() } }
        val lending = Database {
            LendingEntity.new {
                userSub = transaction { FakeUser.provideEntity() }
                from = LocalDate.of(2025, 10, 8)
                to = LocalDate.of(2025, 10, 9)
                returned = true
                memorySubmitted = true
            }
        }

        // Simulate an old (pre-migration) database: add back the legacy columns, and fill them in as the old
        // `add_memory` endpoint used to. Also drop `memories.fromInstant`/`fromZone`/`toInstant`/`toZone` (added
        // later by V7), since at the time V6 runs, that concept doesn't exist yet -- V6's own inserts don't set them.
        Database.exec(
            """
                ALTER TABLE lendings ADD COLUMN memory json;
                ALTER TABLE lendings ADD COLUMN "memoryPdf" uuid;
                ALTER TABLE memories DROP COLUMN "fromInstant";
                ALTER TABLE memories DROP COLUMN "fromZone";
                ALTER TABLE memories DROP COLUMN "toInstant";
                ALTER TABLE memories DROP COLUMN "toZone";
            """.trimIndent()
        ).assertTrue()

        val memoryJson = """
            {
                "place": "Alcoi",
                "members": [${member.memberNumber}],
                "externalUsers": "Jane Doe",
                "text": "Old memory text",
                "sport": "${Sports.ORIENTEERING.name}",
                "department": "${department.id.value}",
                "files": ["${attachmentFile.id.value}"]
            }
        """.trimIndent()
        Database.exec(
            """UPDATE lendings SET memory = ?::json, "memoryPdf" = ?::uuid WHERE id = ?::uuid""",
            arrayOf(memoryJson, pdfFile.id.value.toString(), lending.id.value.toString())
        ).assertTrue()

        // Migrate
        Database { V6.migrate() }

        // Verify the memory was migrated correctly
        Database.execQuery(
            """SELECT place, "externalPeople", "text", sport, department, "submittedBy", lending, pdf FROM memories WHERE lending = '${lending.id.value}'"""
        ).let { rs ->
            assertTrue(rs.next(), "Should have found the migrated memory")
            assertEquals("Alcoi", rs.getString("place"))
            assertEquals("Jane Doe", rs.getString("externalPeople"))
            assertEquals("Old memory text", rs.getString("text"))
            assertEquals(Sports.ORIENTEERING.name, rs.getString("sport"))
            assertEquals(department.id.value.toString(), rs.getString("department"))
            assertEquals(FakeUser.SUB, rs.getString("submittedBy"))
            assertEquals(pdfFile.id.value.toString(), rs.getString("pdf"))
        }

        val memoryId = Database.execQuery("SELECT id FROM memories WHERE lending = '${lending.id.value}'").let { rs ->
            assertTrue(rs.next())
            rs.getString("id")
        }

        Database.execQuery("SELECT member FROM memories_members WHERE memory = '$memoryId'").let { rs ->
            assertTrue(rs.next(), "Should have found the migrated member link")
            assertEquals(member.memberNumber.toLong(), rs.getLong("member"))
            assertFalse(rs.next(), "Should not have found more than one member link")
        }

        Database.execQuery("SELECT file FROM memories_files WHERE memory = '$memoryId'").let { rs ->
            assertTrue(rs.next(), "Should have found the migrated file link")
            assertEquals(attachmentFile.id.value.toString(), rs.getString("file"))
            assertFalse(rs.next(), "Should not have found more than one file link")
        }

        // Verify the legacy columns were dropped
        Database.execQuery(
            "SELECT column_name FROM information_schema.columns WHERE table_name = 'lendings' AND column_name IN ('memory', 'memoryPdf')"
        ).let { rs ->
            assertFalse(rs.next(), "Legacy columns should have been dropped")
        }
    }
}
