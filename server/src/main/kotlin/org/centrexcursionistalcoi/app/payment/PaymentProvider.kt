package org.centrexcursionistalcoi.app.payment

import org.centrexcursionistalcoi.app.data.PaymentMethod
import org.centrexcursionistalcoi.app.data.PaymentReference
import org.centrexcursionistalcoi.app.data.PaymentStatus

/**
 * A service that charges users online (Redsys, Stripe...). The rest of the server only talks to this interface and
 * stores whatever the provider reports, so a provider can be replaced or added without touching the database or the
 * routes. Nothing about how a provider names or formats its payments leaks out: it creates a [PaymentReference] with
 * each payment, and the caller just keeps it and hands it back.
 *
 * The flow is: [createPayment] gives the user somewhere to pay, the provider tells the server the result later
 * ([handleNotification], called from a public route), and [refund] or [cancel] return money if needed.
 *
 * Providers fail with [PaymentGatewayUnavailableException] when they can't be reached, which is transient.
 */
interface PaymentProvider {
    /** Stable name of the provider, e.g. `redsys`. It's the [PaymentReference.provider] of its payments. */
    val id: String

    /** The ways of paying this provider offers, besides [PaymentMethod.ANY]. */
    val methods: Set<PaymentMethod>

    /**
     * Starts a payment of [request] and returns how to send the user to pay for it, with the reference of the payment.
     * The caller stores a `Transactions` row (status [PaymentStatus.PENDING]) with that reference.
     * @throws UnsupportedPaymentMethodException if [PaymentRequest.method] isn't offered.
     */
    suspend fun createPayment(request: PaymentRequest): PaymentSession

    /**
     * Reads a notification the provider sent to the server, checking it really comes from the provider (signature).
     * @throws InvalidPaymentNotificationException if it can't be trusted: nothing in it may be used then.
     */
    suspend fun handleNotification(notification: RawPaymentNotification): PaymentResult

    /**
     * Returns [amountCents] of the payment [reference] to the user. [amountCents] is what was paid (or part of it): the
     * provider can't be assumed to remember it.
     * @throws PaymentProviderException if the provider refuses, or [reference] isn't one of its payments.
     */
    suspend fun refund(reference: PaymentReference, amountCents: Int): PaymentResult

    /**
     * Voids the payment [reference], of [amountCents], before the money moves. A provider that can't void a payment
     * returns the money instead: [PaymentResult.status] tells which happened.
     * @throws PaymentProviderException if the provider refuses, or [reference] isn't one of its payments.
     */
    suspend fun cancel(reference: PaymentReference, amountCents: Int): PaymentResult

    /** Checks [reference] is one of this provider's. */
    fun requireOwn(reference: PaymentReference) {
        if (reference.provider != id) throw PaymentProviderException("The payment ${reference.key} is not from $id")
    }
}
