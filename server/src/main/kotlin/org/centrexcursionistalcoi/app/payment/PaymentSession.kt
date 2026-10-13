package org.centrexcursionistalcoi.app.payment

/** How the user pays for a [PaymentRequest]. */
sealed interface PaymentSession {
    /** The user must be sent to [url] (GET). */
    data class Redirect(val url: String) : PaymentSession

    /** The user must be sent to [url] with a POST of [fields] (Redsys works this way). */
    data class Form(val url: String, val fields: Map<String, String>) : PaymentSession
}
