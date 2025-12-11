package org.centrexcursionistalcoi.app.database.migrations

import org.centrexcursionistalcoi.app.assertTrue
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.PostgresTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestV4Migration : PostgresTestBase() {
    @Test
    fun test() {
        Database.exec(
            """
                CREATE TABLE user_references (
                    sub text NOT NULL,
                    nif text,
                    member bigint NOT NULL,
                    full_name text NOT NULL,
                    email text,
                    groups text[] DEFAULT ARRAY[]::text[] NOT NULL,
                    is_disabled boolean DEFAULT false NOT NULL,
                    password bytea,
                    femecv_username character varying(512),
                    femecv_password character varying(512),
                    femecv_last_sync timestamp without time zone,
                    last_update timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
                    "disableReason" text
                );
            """.trimIndent(),
        ).assertTrue()

        // Insert sample data
        // User without email and password
        Database.exec(
            """INSERT INTO user_references (sub, nif, member, full_name, email, password, groups) VALUES (?, ?, ?, ?, ?, ?, ?);""",
            arrayOf("1", "87654321X", 1, "Test User 1", null, null, arrayOf("user"))
        ).assertTrue()
        // User without password
        Database.exec(
            """INSERT INTO user_references (sub, nif, member, full_name, email, password, groups) VALUES (?, ?, ?, ?, ?, ?, ?);""",
            arrayOf("2", "12345678Z", 2, "Test User 2", "user2@example.com", null, arrayOf("user"))
        ).assertTrue()
        // User with nif, email and password
        Database.exec(
            """INSERT INTO user_references (sub, nif, member, full_name, email, password, groups) VALUES (?, ?, ?, ?, ?, ?, ?);""",
            arrayOf("3", "11223344A", 3, "Test User 3", "user3@example.com", ByteArray(0), arrayOf("user"))
        ).assertTrue()

        // Run migration
        Database {
            V4.migrate()
        }

        // Verify migration - only the last user should exist
        Database.execQuery("SELECT * FROM user_references ORDER BY sub;").let { rs ->
            assertTrue { rs.next() }

            assertEquals("Test User 3", rs.getString("full_name"))
            assertEquals(false, rs.getBoolean("is_disabled"))

            // There should be no more rows
            assertFalse { rs.next() }
        }
    }
}
