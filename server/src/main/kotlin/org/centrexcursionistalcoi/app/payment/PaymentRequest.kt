package org.centrexcursionistalcoi.app.payment

import org.centrexcursionistalcoi.app.data.PaymentMethod

/** What to charge. */
data class PaymentRequest(
    /** In euro cents, as agreed when paying: prices change over time, so it's never recalculated afterwards. */
    val amountCents: Int,
    /** Shown to the user while paying. */
    val description: String,
    /** Where the user lands once done, in the app or web. */
    val successUrl: String,
    val failureUrl: String,
    val method: PaymentMethod = PaymentMethod.ANY,
    /** The user's language (BCP 47), if the provider's page can be localised. */
    val language: String? = null,
    /** The payer's phone with prefix (`+34...`), to prefill it for methods that ask for it (Bizum). */
    val payerPhone: String? = null,
    /** Echoed back by the provider in the notification, if it supports it. */
    val metadata: String? = null,
) {
    init {
        require(amountCents > 0) { "The amount must be positive" }
    }
}
