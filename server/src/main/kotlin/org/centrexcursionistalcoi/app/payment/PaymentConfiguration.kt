package org.centrexcursionistalcoi.app.payment

import org.centrexcursionistalcoi.app.payment.redsys.RedsysConfig
import org.centrexcursionistalcoi.app.payment.redsys.RedsysPaymentProvider
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("PaymentConfiguration")

/**
 * Registers the payment providers configured in the environment. Never fails: a missing or wrong configuration is
 * logged and leaves that provider out, so the server still starts and payments answer "gateway unavailable".
 */
fun configurePayments(getenv: (String) -> String? = System::getenv) {
    try {
        val config = RedsysConfig.fromEnvironment(getenv)
        if (config == null) {
            logger.warn("Redsys is not configured (REDSYS_MERCHANT_CODE, REDSYS_TERMINAL, REDSYS_SECRET_KEY). Payments are unavailable.")
        } else {
            PaymentProviders.register(RedsysPaymentProvider(config))
            logger.info("Redsys configured for ${if (config.production) "production" else "testing"}, Bizum ${if (config.bizum) "on" else "off"}.")
        }
    } catch (e: IllegalArgumentException) {
        logger.error("Redsys is misconfigured, payments are unavailable: ${e.message}")
    }
}
