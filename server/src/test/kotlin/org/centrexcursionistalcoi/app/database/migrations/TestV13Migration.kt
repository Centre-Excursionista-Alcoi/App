package org.centrexcursionistalcoi.app.database.migrations

import kotlin.test.Test
import kotlin.test.assertEquals
import org.centrexcursionistalcoi.app.assertTrue
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.PostgresTestBase
import org.centrexcursionistalcoi.app.database.table.SpaceKeys
import org.jetbrains.exposed.v1.jdbc.selectAll

/**
 * Tests the migration to the inventory of keys: the keys of a space (and what lendings took of them) are replaced by
 * keys of a type, which are not for a single space.
 */
class TestV13Migration : PostgresTestBase() {
    @Test
    fun test() {
        // Create the current schema, then simulate a pre-V13 database: keys of a single space, with a quantity.
        Database.init()
        Database.exec(
            """
                DROP TABLE space_lending_keys;
                DROP TABLE space_keys;
                CREATE TABLE space_keys (
                    id uuid PRIMARY KEY, "lastUpdate" timestamp NOT NULL DEFAULT now(), space uuid NOT NULL,
                    name text NOT NULL, "maxQuantity" integer NOT NULL, "nfcId" bytea
                );
                CREATE TABLE space_lending_keys (
                    lending uuid NOT NULL, key uuid NOT NULL, quantity integer NOT NULL, PRIMARY KEY (lending, key)
                );
            """.trimIndent()
        ).assertTrue()

        // Migrate
        Database { V13.migrate() }

        // They have the shape of the new ones
        val columns = Database.execQuery(
            "SELECT column_name FROM information_schema.columns WHERE table_name = 'space_keys'"
        ).let { rs -> buildSet { while (rs.next()) add(rs.getString("column_name")) } }
        assertEquals(setOf("id", "lastUpdate", "type", "label", "nfcId"), columns)
        val lendingKeys = Database.execQuery(
            "SELECT column_name FROM information_schema.columns WHERE table_name = 'space_lending_keys'"
        ).let { rs -> buildSet { while (rs.next()) add(rs.getString("column_name")) } }
        assertEquals(setOf("lending", "key", "givenBy", "givenAt", "returnedTo", "returnedAt"), lendingKeys)
        assertEquals(0, Database { SpaceKeys.selectAll().count() })
    }
}
