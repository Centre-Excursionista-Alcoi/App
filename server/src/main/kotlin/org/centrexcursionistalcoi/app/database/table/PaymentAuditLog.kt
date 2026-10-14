package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.data.PaymentAuditEvent
import org.centrexcursionistalcoi.app.data.PaymentStatus
import org.centrexcursionistalcoi.app.database.DatabaseNowExpression
import org.centrexcursionistalcoi.app.json
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb
import kotlinx.serialization.json.JsonObject

/**
 * Append-only history of everything that happens with the payments gateway: payments started, what providers report,
 * status changes, refunds and cancellations, and what went wrong. Nothing updates or deletes rows, and nothing
 * references them, so it is kept even if a transaction is deleted: that's why [transactionId] isn't a foreign key.
 *
 * Never store anything sensitive here: [detail] holds what the provider reports sanitized, like `Transactions.payload`.
 */
object PaymentAuditLog : UuidTable("payment_audit_log") {
    val createdAt = timestamp("createdAt").defaultExpression(DatabaseNowExpression)

    /** The transaction it is about. `null` if there is none (an unknown reference, a payment that wasn't started). */
    val transactionId = uuid("transactionId").nullable().index("IX_PaymentAuditLog_transactionId")

    /** `Transactions.referenceKey`, to trace what the provider reported even without a transaction. */
    val referenceKey = text("referenceKey").nullable().index("IX_PaymentAuditLog_referenceKey")
    val provider = text("provider").nullable()
    val event = enumerationByName<PaymentAuditEvent>("event", 40)

    /** Who or what caused it: `user:<sub>`, `provider` (a notification), `system`... */
    val actor = text("actor")
    val previousStatus = enumerationByName<PaymentStatus>("previousStatus", 20).nullable()
    val newStatus = enumerationByName<PaymentStatus>("newStatus", 20).nullable()
    val amountCents = integer("amountCents").nullable()

    /** Anything else worth knowing about the event: the sanitized payload, the error... */
    val detail = jsonb<JsonObject>("detail", json).nullable()
}
