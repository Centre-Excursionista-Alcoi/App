package org.centrexcursionistalcoi.app.data

/** How the user wants to pay. Providers may not support all of them, see `PaymentProvider.methods`. */
enum class PaymentMethod {
    /** The provider offers what it has and the user chooses. */
    ANY,
    CARD,
    BIZUM,
}
