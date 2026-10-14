package org.centrexcursionistalcoi.app.payment

import java.util.concurrent.ConcurrentHashMap
import org.centrexcursionistalcoi.app.data.PaymentMethod
import org.centrexcursionistalcoi.app.data.PaymentReference

/**
 * The payment providers the server has configured. A provider that isn't configured is simply not registered: the
 * server starts anyway, and whatever needs one fails with [PaymentGatewayUnavailableException] (503) until it is.
 */
object PaymentProviders {
    private val providers = ConcurrentHashMap<String, PaymentProvider>()

    /** Makes [provider] available, replacing the one with its [PaymentProvider.id]. */
    fun register(provider: PaymentProvider) {
        providers[provider.id] = provider
    }

    fun unregister(id: String) {
        providers.remove(id)
    }

    fun clear() {
        providers.clear()
    }

    /** The provider with [id], if configured. */
    operator fun get(id: String): PaymentProvider? = providers[id]

    /** The ways of paying the configured providers offer. Empty if there is none. */
    fun methods(): List<PaymentMethod> = providers.values.flatMap { it.methods }.distinct().sortedBy { it.ordinal }

    /** The provider that handles [reference]. */
    fun of(reference: PaymentReference): PaymentProvider =
        providers[reference.provider] ?: throw PaymentGatewayUnavailableException("${reference.provider} is not configured")

    /** The provider to use for new payments. */
    fun default(): PaymentProvider =
        providers.values.minByOrNull { it.id } ?: throw PaymentGatewayUnavailableException("No payment provider is configured")
}
