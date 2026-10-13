package org.centrexcursionistalcoi.app.payment

/** What to charge. */
data class PaymentRequest(
    /** Unique in the provider. The provider may impose a format, so it's created by the provider's own helper. */
    val orderId: String,
    /** In euros. */
    val amount: Double,
    /** Shown to the user while paying. */
    val description: String,
    /** Where the user lands once done, in the app or web. */
    val successUrl: String,
    val failureUrl: String,
    /** The user's language (BCP 47), if the provider's page can be localised. */
    val language: String? = null,
    /** Echoed back by the provider in the notification, if it supports it. */
    val metadata: String? = null,
)
