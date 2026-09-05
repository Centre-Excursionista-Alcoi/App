package org.centrexcursionistalcoi.app.database.migrations

import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

/**
 * Migration V8:
 * - Introduces fine-grained per-department roles (`DepartmentRole`), replacing the single `is_manager` boolean on
 *   `department_members` with a `roles text[]` column (mirroring the existing `groups` array on `user_references`).
 * - Backfill: rows with `is_manager = true` get `roles = ARRAY['admin']`, a deliberate choice to preserve the old
 *   "manager runs the whole department" behavior with no follow-up action required, even though it grants these
 *   existing managers new capabilities (inventory/event/post writes, memory management) they never had access to
 *   under the old boolean. Departments that want a narrower migration for a given member can adjust their roles
 *   afterwards via `PATCH /departments/{id}/members/{memberId}/roles`.
 * - The old `is_manager` column is dropped once backfilled, since every call site has been migrated to read
 *   `roles` instead.
 */
object V8 : DatabaseMigration {
    override val from: Int = 7
    override val to: Int = 8

    context(tr: JdbcTransaction)
    override fun migrate() {
        tr.exec(
            """
                ALTER TABLE department_members ADD COLUMN roles text[] NOT NULL DEFAULT '{}';
            """.trimIndent()
        )

        tr.exec(
            """
                UPDATE department_members
                SET roles = ARRAY['admin']
                WHERE is_manager = true;
            """.trimIndent()
        )

        tr.exec(
            """
                ALTER TABLE department_members DROP COLUMN is_manager;
            """.trimIndent()
        )
    }
}
