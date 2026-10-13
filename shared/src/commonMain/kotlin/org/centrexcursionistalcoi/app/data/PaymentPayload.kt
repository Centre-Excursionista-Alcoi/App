package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.serializer.PaymentPayloadSerializer

/**
 * What a payment provider reported about a payment, as the provider sends it. Each provider has its own
 * implementation (e.g. [RedsysPayload]); the stored JSON carries no type tag: [PaymentPayloadSerializer] tells them
 * apart by their fields, so the database doesn't depend on any provider.
 */
@Serializable(PaymentPayloadSerializer::class)
sealed interface PaymentPayload {
    /** Identifies the payment in the provider, matches `Transactions.orderId`. */
    val orderId: String

    /**
     * A copy that is safe to store: nothing that identifies the payer's card, other than its last four digits to help
     * with support. Providers return only sanitized payloads.
     */
    fun sanitized(): PaymentPayload
}
