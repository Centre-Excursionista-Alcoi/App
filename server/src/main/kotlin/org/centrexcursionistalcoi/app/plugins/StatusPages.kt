package org.centrexcursionistalcoi.app.plugins

import io.ktor.http.HttpHeaders
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.header
import io.sentry.Sentry
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.respondError
import org.centrexcursionistalcoi.app.payment.PaymentGatewayUnavailableException
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("StatusPages")

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<PaymentGatewayUnavailableException> { call, exception ->
            logger.warn("Payment gateway unavailable: ${exception.message}", exception.cause)
            call.response.header(HttpHeaders.RetryAfter, "60")
            call.respondError(Error.PaymentGatewayUnavailable())
        }
        exception<Exception> { call, exception ->
            Sentry.captureException(exception)
            logger.error("Unhandled exception caught", exception)
            call.respondError(Error.Exception(exception))
        }
    }
}
