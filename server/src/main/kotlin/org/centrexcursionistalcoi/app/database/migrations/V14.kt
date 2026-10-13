package org.centrexcursionistalcoi.app.database.migrations

import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

/**
 * Migration V14:
 * - `transactions.redsysPayload` is now `payload` (if it still has the old name): what the payment provider reported is no longer tied to Redsys, the
 *   kind of payload is told from its fields. Rows keep their JSON as it was, which is already how a Redsys payload is
 *   read.
 */
object V14 : DatabaseMigration {
    override val from: Int = 13
    override val to: Int = 14

    context(tr: JdbcTransaction)
    override fun migrate() {
        // A database created by an older migration test starts from the current schema, which already has `payload`
        tr.exec(
            """
                DO $$
                BEGIN
                    IF EXISTS (
                        SELECT 1 FROM information_schema.columns
                        WHERE table_name = 'transactions' AND column_name = 'redsysPayload'
                    ) THEN
                        ALTER TABLE transactions RENAME COLUMN "redsysPayload" TO payload;
                    END IF;
                END $$;
            """.trimIndent()
        )
    }
}
