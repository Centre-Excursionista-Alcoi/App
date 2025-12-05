package org.centrexcursionistalcoi.app.database.migrations

import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

/**
 * Migration V3:
 * - This migration changes the login method from NIF to email.
 * - To do so, users must have an email address set, and it must be unique.
 * - Therefore, remove all users without email, and for users with duplicate emails, keep only the one with the lowest
 *   member number.
 * - Change the nullability of the NIF column to allow nulls.
 * - Change the nullability of the email column to `NOT NULL`.
 * - Add a unique index on the email column.
 */
object V3 : DatabaseMigration {
    override val from: Int = 2
    override val to: Int = 3

    context(tr: JdbcTransaction)
    override fun migrate() {
        tr.exec("""
            -- Delete users without email
            DELETE FROM user_references
            WHERE email IS NULL OR email = '';

            -- Delete duplicate emails, keeping the one with the lowest member number
            DELETE FROM user_references ur1
            USING user_references ur2
            WHERE ur1.email = ur2.email
              AND ur1.member > ur2.member;

            -- Alter NIF column to allow NULLs
            ALTER TABLE user_references
            ALTER COLUMN nif DROP NOT NULL;

            -- Alter email column to NOT NULL
            ALTER TABLE user_references
            ALTER COLUMN email SET NOT NULL;

            -- Add unique index on email column
            CREATE UNIQUE INDEX idx_userreferences_email_unique ON user_references(email);
        """.trimIndent())
    }
}
