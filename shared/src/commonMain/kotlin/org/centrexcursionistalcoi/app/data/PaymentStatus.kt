package org.centrexcursionistalcoi.app.data

enum class PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED,
    REFUNDED,

    /** Voided before the money was taken (or, for methods that can't be voided, returned). */
    CANCELLED,
}
