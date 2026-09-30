package org.centrexcursionistalcoi.app.database.migrations

import org.centrexcursionistalcoi.app.database.table.CredentialKind
import org.centrexcursionistalcoi.app.database.table.UserCredentialRecords
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

/**
 * Migration V12:
 * - WebAuthn credentials can be passkeys as well as restore keys: `user_credential_records` gets a `kind`, a
 *   `name`, and when it was created and last used. Every existing credential is a restore key, created now.
 *
 * The columns are only added if missing: a table created by the current schema already has them.
 */
object V12 : DatabaseMigration {
    override val from: Int = 11
    override val to: Int = 12

    context(tr: JdbcTransaction)
    override fun migrate() {
        val table = tr.identity(UserCredentialRecords)
        val kind = tr.identity(UserCredentialRecords.kind)
        val name = tr.identity(UserCredentialRecords.name)
        val createdAt = tr.identity(UserCredentialRecords.createdAt)
        val lastUsedAt = tr.identity(UserCredentialRecords.lastUsedAt)

        tr.exec(
            """
                ALTER TABLE $table
                    ADD COLUMN IF NOT EXISTS $kind VARCHAR(32) NOT NULL DEFAULT '${CredentialKind.RESTORE_KEY.name}',
                    ADD COLUMN IF NOT EXISTS $name TEXT NULL,
                    ADD COLUMN IF NOT EXISTS $createdAt TIMESTAMP NOT NULL DEFAULT now(),
                    ADD COLUMN IF NOT EXISTS $lastUsedAt TIMESTAMP NULL;
                ALTER TABLE $table ALTER COLUMN $kind DROP DEFAULT, ALTER COLUMN $createdAt DROP DEFAULT;
            """.trimIndent()
        )
    }
}
