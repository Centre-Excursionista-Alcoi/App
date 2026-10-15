package org.centrexcursionistalcoi.app.database.migrations

import kotlin.test.Test
import kotlin.test.assertEquals
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.PostgresTestBase

/**
 * Tests that spaces get the columns for their specs and location, and the table for their photos.
 *
 * Doesn't depend on the current schema: it creates the tables with only what the migration touches.
 */
class TestV18Migration : PostgresTestBase() {
    private fun columns(table: String) = Database.execQuery(
        "SELECT column_name FROM information_schema.columns WHERE table_name = '$table'"
    ).let { rs -> buildSet { while (rs.next()) add(rs.getString("column_name")) } }

    private fun createTables() = Database {
        exec("CREATE TABLE files (id uuid PRIMARY KEY)")
        exec("CREATE TABLE spaces (id uuid PRIMARY KEY)")
        exec("INSERT INTO spaces VALUES (gen_random_uuid())")
    }

    private val expected = setOf("id", "seats", "beds", "address", "latitude", "longitude", "altitude", "energySources", "climateControl")

    @Test
    fun test() {
        createTables()

        Database { V18.migrate() }

        assertEquals(expected, columns("spaces"))
        assertEquals(setOf("space", "file"), columns("space_images"))
        val rs = Database.execQuery("""SELECT seats, "energySources"::text AS energy FROM spaces""")
        assertEquals(true, rs.next())
        rs.getInt("seats")
        assertEquals(true, rs.wasNull())
        assertEquals("[]", rs.getString("energy"))
    }

    @Test
    fun test_runsTwice() {
        createTables()

        Database { V18.migrate() }
        Database { V18.migrate() }

        assertEquals(expected, columns("spaces"))
    }
}
