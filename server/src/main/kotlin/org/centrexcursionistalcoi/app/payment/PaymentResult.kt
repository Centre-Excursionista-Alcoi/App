package org.centrexcursionistalcoi.app.payment

import org.centrexcursionistalcoi.app.data.PaymentPayload
import org.centrexcursionistalcoi.app.data.PaymentReference
import org.centrexcursionistalcoi.app.data.PaymentStatus

/** The verified state of a payment. */
data class PaymentResult(
    val reference: PaymentReference,
    val status: PaymentStatus,
    /** In euro cents, as reported by the provider. */
    val amountCents: Int?,
    /** Stored in the transaction: already [sanitized][PaymentPayload.sanitized]. */
    val payload: PaymentPayload,
)
