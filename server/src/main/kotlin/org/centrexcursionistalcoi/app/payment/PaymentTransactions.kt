package org.centrexcursionistalcoi.app.payment

import kotlin.time.Clock
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.data.PaymentReference
import org.centrexcursionistalcoi.app.data.PaymentStatus
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.table.Transactions
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.slf4j.LoggerFactory

/**
 * Keeps the `Transactions` of the payments started through a [PaymentProvider] and what the provider reports about
 * them. Whatever links a transaction to what is being paid (a lending...) is up to the caller.
 */
object PaymentTransactions {
    private val logger = LoggerFactory.getLogger("PaymentTransactions")

    private val listeners = java.util.concurrent.ConcurrentHashMap<String, suspend (Applied) -> Unit>()

    /**
     * Calls [listener] whenever a transaction changes status, for whatever links it to what is being paid. Registering
     * again under the same [name] replaces the listener.
     */
    fun listen(name: String, listener: suspend (Applied) -> Unit) {
        listeners[name] = listener
    }

    /** A payment that was started: send the user to [session]. */
    data class Started(val transactionId: Uuid, val session: PaymentSession)

    /** What recording a [PaymentResult] did to its transaction. */
    data class Applied(val transactionId: Uuid, val previous: PaymentStatus, val current: PaymentStatus) {
        val changed: Boolean get() = previous != current
    }

    /**
     * Starts a payment of [request] with [provider] and records it as pending.
     * @throws PaymentGatewayUnavailableException if there is no provider, or it can't be reached.
     */
    suspend fun start(
        request: PaymentRequest,
        provider: PaymentProvider = PaymentProviders.default(),
    ): Started {
        var attempt = 0
        while (true) {
            val session = provider.createPayment(request)
            try {
                val id = Database {
                    Transactions.insertAndGetId {
                        it[Transactions.provider] = provider.id
                        it[reference] = session.reference
                        it[referenceKey] = session.reference.key
                        it[amountCents] = request.amountCents
                        it[status] = PaymentStatus.PENDING
                    }.value
                }
                return Started(id, session)
            } catch (e: ExposedSQLException) {
                // The reference of a payment is random: a repeated one is a collision, which another try avoids
                if (++attempt >= MAX_ATTEMPTS) throw e
                logger.warn("Payment reference ${session.reference.key} could not be recorded, trying again", e)
            }
        }
    }

    /**
     * Records what the provider reported, if it moves the transaction forward: a pending one is completed or failed,
     * and a pending or completed one can be refunded or cancelled. Anything else (a repeated notification...) changes
     * nothing, so it's safe to receive it more than once.
     * @return `null` if there is no transaction for the reference.
     */
    suspend fun apply(result: PaymentResult): Applied? {
        val applied = record(result) ?: return null
        if (applied.changed) {
            for ((name, listener) in listeners) {
                try {
                    listener(applied)
                } catch (e: Exception) {
                    // The transaction is recorded: a listener failing must not make the provider retry or the refund fail
                    logger.error("Listener $name failed for transaction ${applied.transactionId}", e)
                }
            }
        }
        return applied
    }

    private fun record(result: PaymentResult): Applied? = Database {
        val row = Transactions.selectAll().where { Transactions.referenceKey eq result.reference.key }.singleOrNull()
            ?: return@Database null
        val id = row[Transactions.id].value
        val previous = row[Transactions.status]
        val next = result.status.takeIf { previous.mayBecome(it) } ?: previous
        if (next != previous) {
            Transactions.update({ Transactions.id eq id }) {
                it[status] = next
                it[payload] = result.payload
                it[updatedAt] = Clock.System.now()
            }
        }
        Applied(id, previous, next)
    }

    /**
     * Returns the money of the completed transaction [transactionId] through its provider, and records it.
     * @throws IllegalStateException if it isn't completed, or there is no such transaction.
     * @throws PaymentProviderException if the provider refuses (or [PaymentGatewayUnavailableException] if it is down).
     */
    suspend fun refund(transactionId: Uuid): PaymentResult {
        val (reference, amountCents) = load(transactionId, PaymentStatus.COMPLETED)
        return PaymentProviders.of(reference).refund(reference, amountCents).also { apply(it) }
    }

    /**
     * Voids the pending or completed transaction [transactionId] through its provider (which may return the money
     * instead, if it can't void), and records it. Same errors as [refund].
     */
    suspend fun cancel(transactionId: Uuid): PaymentResult {
        val (reference, amountCents) = load(transactionId, PaymentStatus.PENDING, PaymentStatus.COMPLETED)
        return PaymentProviders.of(reference).cancel(reference, amountCents).also { apply(it) }
    }

    private fun load(transactionId: Uuid, vararg allowed: PaymentStatus): Pair<PaymentReference, Int> = Database {
        val row = Transactions.selectAll().where { Transactions.id eq transactionId }.singleOrNull()
            ?: throw IllegalStateException("There is no transaction $transactionId")
        check(row[Transactions.status] in allowed) { "The transaction $transactionId is ${row[Transactions.status]}" }
        row[Transactions.reference] to row[Transactions.amountCents]
    }

    private fun PaymentStatus.mayBecome(next: PaymentStatus): Boolean = when (next) {
        PaymentStatus.COMPLETED, PaymentStatus.FAILED -> this == PaymentStatus.PENDING
        PaymentStatus.REFUNDED, PaymentStatus.CANCELLED -> this == PaymentStatus.PENDING || this == PaymentStatus.COMPLETED
        PaymentStatus.PENDING -> false
    }

    private const val MAX_ATTEMPTS = 3
}
