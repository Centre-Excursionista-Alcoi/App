package org.centrexcursionistalcoi.app.database.migrations

import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

/**
 * Migration V15:
 * - `transactions` no longer assumes Redsys: `orderId` becomes a `reference` (the provider's own JSON, with
 *   `referenceKey` to look it up by, unique) and `provider`, and `amount` (euros, as a double) becomes `amountCents`.
 * - Existing rows were all Redsys payments: they get `provider` `redsys`, `referenceKey` `redsys:<orderId>`,
 *   the reference `{"redsys_order": "<orderId>"}` and their amount in cents.
 * - Idempotent, like V12 to V14: a database created with the current schema doesn't have `orderId`.
 */
object V15 : DatabaseMigration {
    override val from: Int = 14
    override val to: Int = 15

    context(tr: JdbcTransaction)
    override fun migrate() {
        tr.exec(
            """
                DO $$
                BEGIN
                    IF EXISTS (
                        SELECT 1 FROM information_schema.columns
                        WHERE table_name = 'transactions' AND column_name = 'orderId'
                    ) THEN
                        ALTER TABLE transactions
                            ADD COLUMN provider text,
                            ADD COLUMN reference jsonb,
                            ADD COLUMN "referenceKey" text,
                            ADD COLUMN "amountCents" integer;
                        UPDATE transactions SET
                            provider = 'redsys',
                            reference = jsonb_build_object('redsys_order', "orderId"),
                            "referenceKey" = 'redsys:' || "orderId",
                            "amountCents" = round(amount * 100)::integer;
                        ALTER TABLE transactions
                            ALTER COLUMN provider SET NOT NULL,
                            ALTER COLUMN reference SET NOT NULL,
                            ALTER COLUMN "referenceKey" SET NOT NULL,
                            ALTER COLUMN "amountCents" SET NOT NULL,
                            DROP COLUMN "orderId",
                            DROP COLUMN amount;
                        CREATE UNIQUE INDEX "UQ_Transactions_referenceKey" ON transactions ("referenceKey");
                    END IF;
                END $$;
            """.trimIndent()
        )
    }
}
