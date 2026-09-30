package org.centrexcursionistalcoi.app.database.migrations

import kotlin.test.Test
import kotlin.test.assertEquals
import org.centrexcursionistalcoi.app.assertTrue
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.PostgresTestBase
import org.centrexcursionistalcoi.app.database.table.CredentialKind
import org.centrexcursionistalcoi.app.database.table.UserCredentialRecords
import org.centrexcursionistalcoi.app.security.AES
import org.centrexcursionistalcoi.app.test.FakeUser
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Tests the migration that tells passkeys and restore keys apart: every credential stored before it is a restore
 * key, created when the migration ran, never used, and unnamed.
 */
class TestV12Migration : PostgresTestBase() {
    @Test
    fun test() {
        AES.initForTests()

        // Create the current schema, then simulate a pre-V12 database: no kind, name or dates.
        Database.init()
        Database { transaction { FakeUser.provideEntity() } }
        Database.exec(
            """
                ALTER TABLE user_credential_records
                    DROP COLUMN kind, DROP COLUMN name, DROP COLUMN created_at, DROP COLUMN last_used_at;
                INSERT INTO user_credential_records (credential_id, "user", attested_credential_data, sign_count)
                    VALUES ('restore-key', '${FakeUser.SUB}', '\x010203', 0);
            """.trimIndent()
        ).assertTrue()

        // Migrate
        Database { V12.migrate() }

        val row = Database { UserCredentialRecords.selectAll().single() }
        assertEquals("restore-key", row[UserCredentialRecords.id].value)
        assertEquals(CredentialKind.RESTORE_KEY, row[UserCredentialRecords.kind])
        assertEquals(null, row[UserCredentialRecords.name])
        assertEquals(null, row[UserCredentialRecords.lastUsedAt])

        // The columns have no defaults left, like the ones Exposed creates.
        Database.execQuery(
            """
                SELECT column_name, column_default FROM information_schema.columns
                WHERE table_name = 'user_credential_records' AND column_name IN ('kind', 'created_at')
            """.trimIndent()
        ).let { rs ->
            var columns = 0
            while (rs.next()) {
                columns++
                assertEquals(null, rs.getString("column_default"), rs.getString("column_name"))
            }
            assertEquals(2, columns)
        }
    }
}
