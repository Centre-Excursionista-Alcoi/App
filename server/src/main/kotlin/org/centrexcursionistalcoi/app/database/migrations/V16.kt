package org.centrexcursionistalcoi.app.database.migrations

import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

/**
 * Migration V16: payments recorded by hand.
 * - `space_lendings."manualPaymentMethod"`: how a manager recorded the payment (cash...), `NULL` if they didn't.
 * - `payment_audit_log.subject`: what an event is about when it has no transaction (`space_lending:<id>`).
 * - Idempotent, like V12 to V15.
 */
object V16 : DatabaseMigration {
    override val from: Int = 15
    override val to: Int = 16

    context(tr: JdbcTransaction)
    override fun migrate() {
        tr.exec("""ALTER TABLE space_lendings ADD COLUMN IF NOT EXISTS "manualPaymentMethod" varchar(20) NULL""")
        tr.exec("""ALTER TABLE payment_audit_log ADD COLUMN IF NOT EXISTS subject text NULL""")
        tr.exec("""CREATE INDEX IF NOT EXISTS "IX_PaymentAuditLog_subject" ON payment_audit_log (subject)""")
    }
}
