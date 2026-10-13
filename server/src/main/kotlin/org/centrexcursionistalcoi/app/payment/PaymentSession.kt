package org.centrexcursionistalcoi.app.payment

import org.centrexcursionistalcoi.app.data.PaymentReference

/** How the user pays for a [PaymentRequest]. */
sealed interface PaymentSession {
    /** Identifies the payment from now on. */
    val reference: PaymentReference

    /** The user must be sent to [url] (GET). */
    data class Redirect(override val reference: PaymentReference, val url: String) : PaymentSession

    /** The user must be sent to [url] with a POST of [fields] (Redsys works this way). */
    data class Form(override val reference: PaymentReference, val url: String, val fields: Map<String, String>) : PaymentSession
}
