package org.centrexcursionistalcoi.app.database.migrations

import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

/**
 * Migration V17: FEMECV credentials that FEMECV rejects.
 * - `user_references."femecvCredentialsInvalidSince"`: since when FEMECV rejects the saved credentials (`NULL` if it doesn't).
 * - `user_references."femecvCredentialsReminderAt"`: when the user was last reminded to enter them again.
 * - Idempotent, like V12 to V16.
 */
object V17 : DatabaseMigration {
    override val from: Int = 16
    override val to: Int = 17

    context(tr: JdbcTransaction)
    override fun migrate() {
        tr.exec("""ALTER TABLE user_references ADD COLUMN IF NOT EXISTS "femecvCredentialsInvalidSince" timestamp NULL""")
        tr.exec("""ALTER TABLE user_references ADD COLUMN IF NOT EXISTS "femecvCredentialsReminderAt" timestamp NULL""")
    }
}
