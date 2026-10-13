package org.centrexcursionistalcoi.app.payment

/** A notification as it arrived, before the provider has checked it. */
data class RawPaymentNotification(
    /** Form or query parameters. */
    val parameters: Map<String, String> = emptyMap(),
    /** The raw body, for providers signing it (needed as received). */
    val body: ByteArray? = null,
    val headers: Map<String, String> = emptyMap(),
) {
    // A data class compares arrays by reference, so the body is compared by content.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RawPaymentNotification) return false
        return parameters == other.parameters && headers == other.headers && body.contentEquals(other.body)
    }

    override fun hashCode(): Int {
        var result = parameters.hashCode()
        result = 31 * result + (body?.contentHashCode() ?: 0)
        result = 31 * result + headers.hashCode()
        return result
    }
}
