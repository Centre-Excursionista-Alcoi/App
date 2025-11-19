package org.centrexcursionistalcoi.app.database.migrations

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.centrexcursionistalcoi.app.assertTrue
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.PostgresTestBase

class TestV2Migration : PostgresTestBase() {
    @Test
    fun test() {
        // Create departments table with integer ID
        Database.exec(
            """
                CREATE TABLE departments (
                    id integer NOT NULL,
                    "displayName" character varying(255) NOT NULL,
                    image uuid
                );
            """.trimIndent(),
            """
                ALTER TABLE ONLY departments ADD CONSTRAINT departments_pkey PRIMARY KEY (id);
            """.trimIndent(),
        ).assertTrue()

        // Insert sample data
        Database.exec(
            """INSERT INTO departments (id, "displayName", image) VALUES (?, ?, ?);""",
            arrayOf(2, "Climbing", null)
        ).assertTrue()

        // Run migration
        Database {
            V2.migrate()
        }

        // Verify migration
        Database.execQuery("SELECT * FROM departments;").let { rs ->
            assertTrue { rs.next() }
            rs.getObject("id", UUID::class.java) // check if id is UUID
            assertFalse { rs.next() }
        }
    }
}
