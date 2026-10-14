package org.centrexcursionistalcoi.app.payment

import org.centrexcursionistalcoi.app.data.PaymentAuditEvent
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.serialization.json.JsonPrimitive
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
     * @param actor who is paying, for the audit log (see [PaymentAudit]).
     * @throws PaymentGatewayUnavailableException if there is no provider, or it can't be reached.
     */
    suspend fun start(
        request: PaymentRequest,
        provider: PaymentProvider = PaymentProviders.default(),
        actor: String = PaymentAudit.ACTOR_SYSTEM,
    ): Started {
        var attempt = 0
        while (true) {
            val session = try {
                provider.createPayment(request)
            } catch (e: Exception) {
                PaymentAudit.tryRecord(
                    PaymentAuditEvent.CREATE_FAILED, actor,
                    provider = provider.id, amountCents = request.amountCents, detail = PaymentAudit.errorDetail(e),
                )
                throw e
            }
            try {
                val id = Database {
                    val id = Transactions.insertAndGetId {
                        it[Transactions.provider] = provider.id
                        it[reference] = session.reference
                        it[referenceKey] = session.reference.key
                        it[amountCents] = request.amountCents
                        it[status] = PaymentStatus.PENDING
                    }.value
                    PaymentAudit.record(
                        PaymentAuditEvent.CREATED, actor,
                        transactionId = id, referenceKey = session.reference.key, provider = provider.id,
                        newStatus = PaymentStatus.PENDING, amountCents = request.amountCents,
                        detail = PaymentAudit.detailOf(
                            "description" to request.description,
                            "method" to request.method.name,
                            "language" to request.language,
                        ),
                    )
                    id
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
     * nothing, so it's safe to receive it more than once. Everything reported is kept in the audit log.
     * @param actor who caused it, for the audit log: the provider by default, as it is who notifies.
     * @return `null` if there is no transaction for the reference.
     */
    suspend fun apply(result: PaymentResult, actor: String = PaymentAudit.ACTOR_PROVIDER): Applied? {
        val applied = record(result, actor) ?: return null
        if (applied.changed) {
            for ((name, listener) in listeners) {
                try {
                    listener(applied)
                } catch (e: Exception) {
                    // The transaction is recorded: a listener failing must not make the provider retry or the refund fail
                    logger.error("Listener $name failed for transaction ${applied.transactionId}", e)
                    PaymentAudit.tryRecord(
                        PaymentAuditEvent.LISTENER_FAILED, PaymentAudit.ACTOR_SYSTEM,
                        transactionId = applied.transactionId, referenceKey = result.reference.key,
                        previousStatus = applied.previous, newStatus = applied.current,
                        detail = PaymentAudit.errorDetail(e, mapOf("listener" to JsonPrimitive(name))),
                    )
                }
            }
        }
        return applied
    }

    private fun record(result: PaymentResult, actor: String): Applied? = Database {
        val key = result.reference.key
        val row = Transactions.selectAll().where { Transactions.referenceKey eq key }.singleOrNull()
        if (row == null) {
            PaymentAudit.record(
                PaymentAuditEvent.UNKNOWN_REFERENCE, actor,
                referenceKey = key, provider = result.reference.provider,
                newStatus = result.status, amountCents = result.amountCents,
                detail = PaymentAudit.payloadDetail(result.payload),
            )
            return@Database null
        }
        val id = row[Transactions.id].value
        val previous = row[Transactions.status]
        val next = result.status.takeIf { previous.mayBecome(it) } ?: previous
        PaymentAudit.record(
            PaymentAuditEvent.RESULT_RECEIVED, actor,
            transactionId = id, referenceKey = key, provider = row[Transactions.provider],
            previousStatus = previous, newStatus = result.status, amountCents = result.amountCents,
            detail = PaymentAudit.payloadDetail(result.payload),
        )
        if (next != previous) {
            Transactions.update({ Transactions.id eq id }) {
                it[status] = next
                it[payload] = result.payload
                it[updatedAt] = Clock.System.now()
            }
        }
        PaymentAudit.record(
            if (next != previous) PaymentAuditEvent.STATUS_CHANGED else PaymentAuditEvent.RESULT_IGNORED, actor,
            transactionId = id, referenceKey = key, provider = row[Transactions.provider],
            previousStatus = previous, newStatus = next, amountCents = row[Transactions.amountCents],
        )
        Applied(id, previous, next)
    }

    /**
     * Returns the money of the completed transaction [transactionId] through its provider, and records it.
     * @param actor who asked for it, for the audit log.
     * @throws IllegalStateException if it isn't completed, or there is no such transaction.
     * @throws PaymentProviderException if the provider refuses (or [PaymentGatewayUnavailableException] if it is down).
     */
    suspend fun refund(transactionId: Uuid, actor: String = PaymentAudit.ACTOR_SYSTEM): PaymentResult =
        operate(PaymentAuditEvent.REFUND_REQUESTED, transactionId, actor, PaymentStatus.COMPLETED) { provider, reference, amountCents ->
            provider.refund(reference, amountCents)
        }

    /**
     * Voids the pending or completed transaction [transactionId] through its provider (which may return the money
     * instead, if it can't void), and records it. Same errors as [refund].
     */
    suspend fun cancel(transactionId: Uuid, actor: String = PaymentAudit.ACTOR_SYSTEM): PaymentResult =
        operate(PaymentAuditEvent.CANCEL_REQUESTED, transactionId, actor, PaymentStatus.PENDING, PaymentStatus.COMPLETED) { provider, reference, amountCents ->
            provider.cancel(reference, amountCents)
        }

    private suspend fun operate(
        requested: PaymentAuditEvent,
        transactionId: Uuid,
        actor: String,
        vararg allowed: PaymentStatus,
        run: suspend (PaymentProvider, PaymentReference, Int) -> PaymentResult,
    ): PaymentResult {
        val loaded = try {
            load(transactionId, *allowed)
        } catch (e: IllegalStateException) {
            PaymentAudit.tryRecord(
                PaymentAuditEvent.OPERATION_FAILED, actor,
                transactionId = transactionId, detail = PaymentAudit.errorDetail(e, mapOf("requested" to JsonPrimitive(requested.name))),
            )
            throw e
        }
        // Recorded before asking the provider: if the audit can't be written, the money doesn't move
        PaymentAudit.record(
            requested, actor,
            transactionId = transactionId, referenceKey = loaded.reference.key, provider = loaded.reference.provider,
            previousStatus = loaded.status, amountCents = loaded.amountCents,
        )
        val result = try {
            run(PaymentProviders.of(loaded.reference), loaded.reference, loaded.amountCents)
        } catch (e: Exception) {
            PaymentAudit.tryRecord(
                PaymentAuditEvent.OPERATION_FAILED, actor,
                transactionId = transactionId, referenceKey = loaded.reference.key, provider = loaded.reference.provider,
                previousStatus = loaded.status, amountCents = loaded.amountCents,
                detail = PaymentAudit.errorDetail(e, mapOf("requested" to JsonPrimitive(requested.name))),
            )
            throw e
        }
        apply(result, actor)
        return result
    }

    private class Loaded(val reference: PaymentReference, val amountCents: Int, val status: PaymentStatus)

    private fun load(transactionId: Uuid, vararg allowed: PaymentStatus): Loaded = Database {
        val row = Transactions.selectAll().where { Transactions.id eq transactionId }.singleOrNull()
            ?: throw IllegalStateException("There is no transaction $transactionId")
        check(row[Transactions.status] in allowed) { "The transaction $transactionId is ${row[Transactions.status]}" }
        Loaded(row[Transactions.reference], row[Transactions.amountCents], row[Transactions.status])
    }

    private fun PaymentStatus.mayBecome(next: PaymentStatus): Boolean = when (next) {
        PaymentStatus.COMPLETED, PaymentStatus.FAILED -> this == PaymentStatus.PENDING
        PaymentStatus.REFUNDED, PaymentStatus.CANCELLED -> this == PaymentStatus.PENDING || this == PaymentStatus.COMPLETED
        PaymentStatus.PENDING -> false
    }

    private const val MAX_ATTEMPTS = 3
}
