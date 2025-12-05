package org.centrexcursionistalcoi.app.database.migrations

import org.centrexcursionistalcoi.app.assertTrue
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.PostgresTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestV3Migration : PostgresTestBase() {
    @Test
    fun test() {
        Database.exec(
            """
                CREATE TABLE user_references (
                    sub text NOT NULL,
                    nif text NOT NULL,
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
                    "disableReason" text,
                    CONSTRAINT chk_user_references_unsigned_integer_member CHECK (((member >= 0) AND (member <= '4294967295'::bigint)))
                );
            """.trimIndent(),
        ).assertTrue()

        // Insert sample data
        // User without email
        Database.exec(
            """INSERT INTO user_references (sub, nif, member, full_name, email, groups) VALUES (?, ?, ?, ?, ?, ?);""",
            arrayOf("abcdef", "87654321X", 1, "Test User 1", null, arrayOf("user"))
        ).assertTrue()
        // User with email
        Database.exec(
            """INSERT INTO user_references (sub, nif, member, full_name, email, groups) VALUES (?, ?, ?, ?, ?, ?);""",
            arrayOf("abcdef", "12345678Z", 3, "Test User 2", "email@example.com", arrayOf("user"))
        ).assertTrue()
        // Duplicate email
        Database.exec(
            """INSERT INTO user_references (sub, nif, member, full_name, email, groups) VALUES (?, ?, ?, ?, ?, ?);""",
            arrayOf("abcdef", "11111111H", 2, "Test User 3", "email@example.com", arrayOf("user"))
        ).assertTrue()

        // Run migration
        Database {
            V3.migrate()
        }

        // Verify migration
        Database.execQuery("SELECT * FROM user_references;").let { rs ->
            assertTrue { rs.next() }
            // "Test User 1" had no email, so it should have been removed
            // "Test User 2" would be present, but "Test User 3" has a lower member number, so it should be kept instead
            // First row should be "Test User 3"
            assertEquals("Test User 3", rs.getString("full_name"))
            // There should be no more rows
            assertFalse { rs.next() }
        }
    }
}
