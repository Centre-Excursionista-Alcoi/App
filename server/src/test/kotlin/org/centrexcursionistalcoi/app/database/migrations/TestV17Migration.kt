package org.centrexcursionistalcoi.app.database.migrations

import kotlin.test.Test
import kotlin.test.assertEquals
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.PostgresTestBase

/**
 * Tests that users get the columns for FEMECV credentials that FEMECV rejects.
 *
 * Doesn't depend on the current schema: it creates `user_references` with only what the migration touches.
 */
class TestV17Migration : PostgresTestBase() {
    private fun columns() = Database.execQuery(
        "SELECT column_name FROM information_schema.columns WHERE table_name = 'user_references'"
    ).let { rs -> buildSet { while (rs.next()) add(rs.getString("column_name")) } }

    private fun createTable() = Database {
        exec("CREATE TABLE user_references (sub varchar(100) PRIMARY KEY)")
        exec("INSERT INTO user_references VALUES ('someone')")
    }

    @Test
    fun test() {
        createTable()

        Database { V17.migrate() }

        assertEquals(setOf("sub", "femecvCredentialsInvalidSince", "femecvCredentialsReminderAt"), columns())
        val rs = Database.execQuery("""SELECT "femecvCredentialsInvalidSince", "femecvCredentialsReminderAt" FROM user_references""")
        assertEquals(true, rs.next())
        assertEquals(null, rs.getTimestamp("femecvCredentialsInvalidSince"))
        assertEquals(null, rs.getTimestamp("femecvCredentialsReminderAt"))
    }

    @Test
    fun test_runsTwice() {
        createTable()

        Database { V17.migrate() }
        Database { V17.migrate() }

        assertEquals(setOf("sub", "femecvCredentialsInvalidSince", "femecvCredentialsReminderAt"), columns())
    }
}
