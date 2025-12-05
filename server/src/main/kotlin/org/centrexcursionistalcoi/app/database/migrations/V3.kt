package org.centrexcursionistalcoi.app.database.migrations

import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

/**
 * Migration V3:
 * - This migration changes the login method from NIF to email.
 * - To do so, users must have an email address set, and it must be unique.
 * - Therefore, disable all users without email, and for users with duplicate emails, keep only the one with the lowest
 *   member number. So set the email of all the other users to null.
 * - Change the nullability of the NIF column to allow nulls.
 * - Add a unique index on the email column.
 */
object V3 : DatabaseMigration {
    override val from: Int = 2
    override val to: Int = 3

    context(tr: JdbcTransaction)
    override fun migrate() {
        tr.exec("""
            -- Disable users without email
            UPDATE user_references
            SET is_disabled = TRUE,
                "disableReason" = 'invalid_email'
            WHERE email IS NULL;
        """.trimIndent())
        tr.exec("""
            -- Disable users and remove email with duplicate emails, keeping only the one with the lowest member number
            WITH ranked_users AS (
                SELECT
                    sub,
                    email,
                    member,
                    ROW_NUMBER() OVER (PARTITION BY email ORDER BY member) AS rn
                FROM user_references
                WHERE email IS NOT NULL
            )
            UPDATE user_references ur
            SET is_disabled = TRUE,
                "disableReason" = 'duplicate_email',
                email = NULL
            FROM ranked_users ru
            WHERE ur.sub = ru.sub AND ru.rn > 1;
        """.trimIndent())
        tr.exec("""
            -- Alter NIF column to allow NULLs
            ALTER TABLE user_references
            ALTER COLUMN nif DROP NOT NULL;
        """.trimIndent())
        tr.exec("""
            -- Add unique index on email column
            CREATE UNIQUE INDEX idx_userreferences_email_unique ON user_references(email);
        """.trimIndent())
    }
}
