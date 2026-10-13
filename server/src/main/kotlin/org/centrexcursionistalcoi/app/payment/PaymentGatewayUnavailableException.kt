package org.centrexcursionistalcoi.app.payment

/**
 * Payments can't be taken now: no provider is configured, or the provider couldn't be reached. Transient, unlike
 * [PaymentProviderException]s in general: it's answered to the app as `Error.PaymentGatewayUnavailable` (503).
 */
class PaymentGatewayUnavailableException(message: String = "Payment gateway unavailable", cause: Throwable? = null) :
    PaymentProviderException(message, cause)
