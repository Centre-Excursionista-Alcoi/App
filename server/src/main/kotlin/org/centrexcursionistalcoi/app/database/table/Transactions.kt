package org.centrexcursionistalcoi.app.database.table

import org.centrexcursionistalcoi.app.data.PaymentPayload
import org.centrexcursionistalcoi.app.data.PaymentReference
import org.centrexcursionistalcoi.app.data.PaymentStatus
import org.centrexcursionistalcoi.app.database.DatabaseNowExpression
import org.centrexcursionistalcoi.app.json
import org.centrexcursionistalcoi.app.serializer.PaymentPayloadSerializer
import org.centrexcursionistalcoi.app.serializer.PaymentReferenceSerializer
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb

/**
 * A payment attempt through a payment provider. Nothing here is tied to a provider: [reference] and [payload] are
 * stored as the provider's own JSON, and told apart by their fields.
 */
object Transactions : UuidTable("transactions") {
    val createdAt = timestamp("createdAt").defaultExpression(DatabaseNowExpression)
    val updatedAt = timestamp("updatedAt").defaultExpression(DatabaseNowExpression)

    /** The `id` of the `PaymentProvider` that handles it, see [PaymentReference.provider]. */
    val provider = text("provider")

    /** Identifies the payment in the provider. Opaque: it's handed back to the provider to refund or cancel. */
    val reference = jsonb("reference", json, PaymentReferenceSerializer)

    /** [PaymentReference.key], to look a transaction up by the reference a notification carries. */
    val referenceKey = text("referenceKey").uniqueIndex("UQ_Transactions_referenceKey")

    /**
     * What was charged, in euro cents, when the payment was started. Prices change over time, so this is the amount
     * agreed then, not something to recalculate.
     */
    val amountCents = integer("amountCents")
    val status = enumerationByName<PaymentStatus>("status", 20)

    /** What the payment provider reported, in its own format (see [PaymentPayload]). Sanitized. */
    val payload = jsonb("payload", json, PaymentPayloadSerializer).nullable()
}
