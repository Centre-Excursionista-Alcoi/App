package org.centrexcursionistalcoi.app.database.migrations

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.centrexcursionistalcoi.app.assertTrue
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.PostgresTestBase
import org.centrexcursionistalcoi.app.database.entity.DepartmentEntity
import org.centrexcursionistalcoi.app.security.AES
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.test.FakeUser2
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Tests the migration that replaces the single `is_manager` boolean on `department_members` with a `roles text[]`
 * column. Rows with `is_manager = true` should backfill to `roles = {admin}` (a deliberate choice to preserve
 * "manager runs the whole department" behavior); other rows should backfill to an empty array. The old
 * `is_manager` column should no longer exist afterwards.
 */
class TestV8Migration : PostgresTestBase() {
    @Test
    fun test() {
        AES.initForTests()

        // Create the current schema (already includes the `roles` column on `department_members`).
        Database.init()

        Database { transaction { FakeUser.provideEntity() } }
        Database { transaction { FakeUser2.provideEntity() } }
        val department = Database { DepartmentEntity.new { displayName = "Test Department" } }

        // Simulate a pre-V8 database: `roles` doesn't exist yet, `is_manager` does.
        Database.exec(
            """
                ALTER TABLE department_members DROP COLUMN roles;
                ALTER TABLE department_members ADD COLUMN is_manager boolean NOT NULL DEFAULT false;
            """.trimIndent()
        ).assertTrue()

        val managerMemberId = UUID.randomUUID()
        Database.exec(
            """INSERT INTO department_members (id, sub, department_id, confirmed, is_manager) VALUES (?::uuid, ?, ?::uuid, true, true)""",
            arrayOf(managerMemberId.toString(), FakeUser.SUB, department.id.value.toString())
        ).assertTrue()

        val regularMemberId = UUID.randomUUID()
        Database.exec(
            """INSERT INTO department_members (id, sub, department_id, confirmed, is_manager) VALUES (?::uuid, ?, ?::uuid, true, false)""",
            arrayOf(regularMemberId.toString(), FakeUser2.SUB, department.id.value.toString())
        ).assertTrue()

        // Migrate
        Database { V8.migrate() }

        // The manager row backfills to roles = {admin}.
        Database.execQuery(
            """SELECT roles FROM department_members WHERE id = '$managerMemberId'"""
        ).let { rs ->
            assertTrue(rs.next())
            val roles = rs.getArray("roles").array as Array<*>
            assertEquals(listOf("admin"), roles.toList())
        }

        // The non-manager row backfills to an empty roles array.
        Database.execQuery(
            """SELECT roles FROM department_members WHERE id = '$regularMemberId'"""
        ).let { rs ->
            assertTrue(rs.next())
            val roles = rs.getArray("roles").array as Array<*>
            assertEquals(emptyList<String>(), roles.toList())
        }

        // The old is_manager column no longer exists.
        Database.execQuery(
            """
                SELECT column_name FROM information_schema.columns
                WHERE table_name = 'department_members' AND column_name = 'is_manager'
            """.trimIndent()
        ).let { rs ->
            assertTrue(!rs.next())
        }
    }
}
