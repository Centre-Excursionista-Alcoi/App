package org.centrexcursionistalcoi.app.database.migrations

import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

object V1 : DatabaseMigration {
    override val from: Int = 0
    override val to: Int = 1

    context(tr: JdbcTransaction)
    override fun migrate() {
        // 1. Rename the column
        tr.exec("ALTER TABLE inventory_item_types RENAME COLUMN category TO categories;")

        // 2. Change the column type from text → text[]
        tr.exec("""
            ALTER TABLE inventory_item_types
            ALTER COLUMN categories TYPE text[]
            USING
              CASE
                WHEN categories IS NULL OR categories = '' THEN NULL
                ELSE ARRAY[categories]
            END;
        """.trimIndent())
    }
}
