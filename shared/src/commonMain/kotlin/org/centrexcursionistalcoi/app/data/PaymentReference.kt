package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable
import org.centrexcursionistalcoi.app.serializer.PaymentReferenceSerializer

/**
 * Identifies a payment in the provider that handles it. Opaque for whoever holds it: it's created by the provider with
 * the payment, stored with the transaction, and handed back to the same provider to refund or cancel it, so nothing
 * outside the provider needs to know how it names payments (e.g. Redsys's order number format).
 *
 * Like [PaymentPayload], it's stored with no type tag: [PaymentReferenceSerializer] tells the providers apart by fields.
 */
@Serializable(PaymentReferenceSerializer::class)
sealed interface PaymentReference {
    /** The `id` of the `PaymentProvider` this belongs to. */
    val provider: String

    /** Identifies the payment among all the ones of all providers: unique, and what a transaction is looked up by. */
    val key: String
}
