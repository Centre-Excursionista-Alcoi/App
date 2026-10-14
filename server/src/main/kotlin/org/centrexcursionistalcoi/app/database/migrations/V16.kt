package org.centrexcursionistalcoi.app.database.migrations

import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

/**
 * Migration V16: adds `payment_audit_log`, the append-only history of everything that happens with the payments gateway.
 * Idempotent, like V12 to V15: a database created with the current schema already has it.
 */
object V16 : DatabaseMigration {
    override val from: Int = 15
    override val to: Int = 16

    context(tr: JdbcTransaction)
    override fun migrate() {
        tr.exec(
            """
                CREATE TABLE IF NOT EXISTS payment_audit_log (
                    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                    "createdAt" timestamp NOT NULL DEFAULT now(),
                    "transactionId" uuid NULL,
                    "referenceKey" text NULL,
                    provider text NULL,
                    event varchar(40) NOT NULL,
                    actor text NOT NULL,
                    "previousStatus" varchar(20) NULL,
                    "newStatus" varchar(20) NULL,
                    "amountCents" integer NULL,
                    detail jsonb NULL
                )
            """.trimIndent()
        )
        tr.exec("""CREATE INDEX IF NOT EXISTS "IX_PaymentAuditLog_transactionId" ON payment_audit_log ("transactionId")""")
        tr.exec("""CREATE INDEX IF NOT EXISTS "IX_PaymentAuditLog_referenceKey" ON payment_audit_log ("referenceKey")""")
    }
}
