package org.centrexcursionistalcoi.app.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveParameters
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.centrexcursionistalcoi.app.data.RedsysReference
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.error.respondError
import org.centrexcursionistalcoi.app.payment.InvalidPaymentNotificationException
import org.centrexcursionistalcoi.app.payment.PaymentGatewayUnavailableException
import org.centrexcursionistalcoi.app.payment.PaymentProviders
import org.centrexcursionistalcoi.app.payment.PaymentTransactions
import org.centrexcursionistalcoi.app.payment.RawPaymentNotification
import org.centrexcursionistalcoi.app.routes.Api
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("PaymentRoutes")

/**
 * What payment providers call. Public: a notification is trusted by its signature, never by who sent it.
 */
fun Route.paymentRoutes() {
    post<Api.Payments.RedsysNotification> {
        val provider = PaymentProviders[RedsysReference.PROVIDER]
            ?: throw PaymentGatewayUnavailableException("Redsys is not configured")
        val parameters = call.receiveParameters().entries().associate { (name, values) -> name to values.first() }

        val result = try {
            provider.handleNotification(RawPaymentNotification(parameters = parameters))
        } catch (e: InvalidPaymentNotificationException) {
            logger.warn("Rejected a Redsys notification: ${e.message}")
            return@post respondError(Error.MalformedRequest())
        }

        val applied = PaymentTransactions.apply(result)
        if (applied == null) {
            logger.warn("Redsys notified a payment with no transaction: ${result.reference.key}")
            return@post respondError(Error.EntityNotFound("Transaction", result.reference.key))
        }
        logger.info("Transaction ${applied.transactionId}: ${applied.previous} -> ${applied.current}")
        call.respond(HttpStatusCode.OK)
    }
}
