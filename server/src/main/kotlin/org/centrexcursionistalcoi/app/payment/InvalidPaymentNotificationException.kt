package org.centrexcursionistalcoi.app.payment

/** A notification doesn't come from the provider (bad signature) or is malformed: nothing in it can be trusted. */
class InvalidPaymentNotificationException(message: String) : PaymentProviderException(message)
