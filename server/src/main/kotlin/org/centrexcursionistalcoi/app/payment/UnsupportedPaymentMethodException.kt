package org.centrexcursionistalcoi.app.payment

import org.centrexcursionistalcoi.app.data.PaymentMethod

/** The provider doesn't offer [method] (e.g. Bizum isn't enabled). */
class UnsupportedPaymentMethodException(val method: PaymentMethod) :
    PaymentProviderException("The payment method $method is not supported")
