package org.centrexcursionistalcoi.app.data

import kotlinx.serialization.Serializable

/** How a payment made outside the payments gateway was received, as a manager records it. */
@Serializable
enum class ManualPaymentMethod {
    CASH,
    CARD,
    BIZUM,
    TRANSFER,
    OTHER,
}
