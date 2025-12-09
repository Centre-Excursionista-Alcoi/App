package org.centrexcursionistalcoi.app.database.migrations

import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

/**
 * Migration V3:
 * - This migration cleans up the UserReferences table by removing all non-registered and invalid users:
 *   - Users without password.
 *   - Users without a valid NIF.
 *   - Users without a valid email.
 * - Exposed will create automatically the new Members table.
 * - The `disableReason` column in UserReferences is removed.
 * - The `nif` column in UserReferences is now non-nullable.
 * - The `email` column in UserReferences is now non-nullable.
 * - The `password` column in UserReferences is now non-nullable.
 */
object V4 : DatabaseMigration {
    override val from: Int = 3
    override val to: Int = 4

    context(tr: JdbcTransaction)
    override fun migrate() {
        tr.exec("""
            -- Remove all users without password, nif or email
            DELETE FROM user_references
            WHERE password IS NULL OR nif IS NULL OR email IS NULL;
        """.trimIndent())
        tr.exec("""
            -- Remove all users with invalid email
            DELETE FROM user_references
            WHERE email !~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$';
        """.trimIndent())
        tr.exec("""
            -- Remove all users with invalid NIF
            DELETE FROM user_references
            WHERE 
                -- Delete anything that doesn't match the basic structure (8 digits/letters + 1 letter)
                -- Trust that the NIFs are well formed regarding control letters
                upper(nif) !~ '^[XYZ0-9]\d{7}[TRWAGMYFPDXBNJZSQVHLCKE]$'
        """.trimIndent())
        tr.exec("""
            -- Remove disableReason column
            ALTER TABLE user_references
            DROP COLUMN "disableReason";
        """.trimIndent())
        tr.exec("""
            -- Set nif, email and password columns to NOT NULL
            ALTER TABLE user_references
            ALTER COLUMN "nif" SET NOT NULL,
            ALTER COLUMN "email" SET NOT NULL,
            ALTER COLUMN "password" SET NOT NULL;
        """.trimIndent())
    }
}
