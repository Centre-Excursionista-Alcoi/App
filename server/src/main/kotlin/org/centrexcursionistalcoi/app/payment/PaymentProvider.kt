package org.centrexcursionistalcoi.app.payment

import org.centrexcursionistalcoi.app.data.PaymentStatus

/**
 * A service that charges users online (Redsys, Stripe...). The rest of the server only talks to this interface and
 * stores whatever the provider reports as a `PaymentPayload`, so a provider can be replaced or added without touching
 * the database or the routes.
 *
 * The flow is: [createPayment] gives the user somewhere to pay, the provider tells the server the result later
 * ([handleNotification], called from a public route), and [refund] returns money if needed.
 */
interface PaymentProvider {
    /** Stable name of the provider, e.g. `redsys`. */
    val id: String

    /**
     * Starts a payment of [request] and returns how to send the user to pay for it.
     * The caller stores a `Transactions` row (status [PaymentStatus.PENDING]) with [PaymentRequest.orderId].
     */
    suspend fun createPayment(request: PaymentRequest): PaymentSession

    /**
     * Reads a notification the provider sent to the server, checking it really comes from the provider (signature).
     * @throws InvalidPaymentNotificationException if it can't be trusted: nothing in it may be used then.
     */
    suspend fun handleNotification(notification: RawPaymentNotification): PaymentResult

    /**
     * Asks the provider for the current state of the payment [orderId], for when a notification never arrived.
     * @return `null` if the provider doesn't know the order.
     */
    suspend fun checkPayment(orderId: String): PaymentResult?

    /**
     * Returns [amount] of the payment [orderId] to the user, all of it if `null`.
     * @throws PaymentProviderException if the provider refuses.
     */
    suspend fun refund(orderId: String, amount: Double? = null): PaymentResult
}
