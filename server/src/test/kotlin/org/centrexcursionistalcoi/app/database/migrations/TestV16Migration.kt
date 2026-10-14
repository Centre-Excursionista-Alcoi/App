package org.centrexcursionistalcoi.app.database.migrations

import kotlin.test.Test
import kotlin.test.assertEquals
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.PostgresTestBase

/**
 * Tests that lendings and the audit log get the columns for payments recorded by hand.
 *
 * Doesn't depend on the current schema: it creates the two tables with only what the migration touches.
 */
class TestV16Migration : PostgresTestBase() {
    private fun columns(table: String) = Database.execQuery(
        "SELECT column_name FROM information_schema.columns WHERE table_name = '$table'"
    ).let { rs -> buildSet { while (rs.next()) add(rs.getString("column_name")) } }

    private fun createTables() = Database {
        exec("CREATE TABLE space_lendings (id uuid PRIMARY KEY)")
        exec("CREATE TABLE payment_audit_log (id uuid PRIMARY KEY)")
        exec("INSERT INTO space_lendings VALUES (gen_random_uuid())")
    }

    @Test
    fun test() {
        createTables()

        Database { V16.migrate() }

        assertEquals(setOf("id", "manualPaymentMethod"), columns("space_lendings"))
        assertEquals(setOf("id", "subject"), columns("payment_audit_log"))
        val rs = Database.execQuery("""SELECT "manualPaymentMethod" FROM space_lendings""")
        assertEquals(true, rs.next())
        assertEquals(null, rs.getString("manualPaymentMethod"))
    }

    @Test
    fun test_runsTwice() {
        createTables()

        Database { V16.migrate() }
        Database { V16.migrate() }

        assertEquals(setOf("id", "manualPaymentMethod"), columns("space_lendings"))
    }
}
