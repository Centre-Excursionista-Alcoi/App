package org.centrexcursionistalcoi.app.payment

/** A [PaymentProvider] couldn't do what was asked of it. */
open class PaymentProviderException(message: String, cause: Throwable? = null) : Exception(message, cause)
