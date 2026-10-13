package org.centrexcursionistalcoi.app.payment

import org.centrexcursionistalcoi.app.data.PaymentPayload
import org.centrexcursionistalcoi.app.data.PaymentStatus

/** The verified state of a payment. */
data class PaymentResult(
    val orderId: String,
    val status: PaymentStatus,
    /** In euros, as reported by the provider. */
    val amount: Double?,
    /** Stored in the transaction. */
    val payload: PaymentPayload,
)
