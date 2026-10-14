package org.centrexcursionistalcoi.app.payment

import io.sentry.Sentry
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.data.PaymentStatus
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.SpaceLendingEntity
import org.centrexcursionistalcoi.app.database.table.SpaceLendingTransactions
import org.centrexcursionistalcoi.app.database.table.Transactions
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.slf4j.LoggerFactory

/**
 * Ties the payments of space lendings (`SpaceLendingTransactions`) to the lending's own payment status: a lending is
 * paid when one of its transactions completes, and refunded when that one is refunded.
 */
object SpaceLendingPayments {
    private val logger = LoggerFactory.getLogger("SpaceLendingPayments")

    private val settled = setOf(PaymentStatus.COMPLETED, PaymentStatus.REFUNDED)

    /** Starts keeping lendings up to date with their transactions. Safe to call more than once. */
    fun register() = PaymentTransactions.listen("space-lendings", ::onChange)

    /** Links a transaction that was just started to the lending it pays. */
    fun link(lendingId: Uuid, transactionId: Uuid) = Database {
        SpaceLendingTransactions.insert {
            it[lending] = lendingId
            it[transaction] = transactionId
        }
    }

    /** Every transaction started for the lending, whatever came of it. */
    fun transactionsOf(lendingId: Uuid): List<Uuid> = Database {
        SpaceLendingTransactions.selectAll()
            .where { SpaceLendingTransactions.lending eq lendingId }
            .map { it[SpaceLendingTransactions.transaction].value }
    }

    /** The transaction of [lendingId] that was paid, if any: the latest one. */
    fun completedTransaction(lendingId: Uuid): Uuid? = Database {
        (SpaceLendingTransactions innerJoin Transactions).selectAll()
            .where { (SpaceLendingTransactions.lending eq lendingId) and (Transactions.status eq PaymentStatus.COMPLETED) }
            .orderBy(Transactions.createdAt, SortOrder.DESC)
            .firstOrNull()?.get(Transactions.id)?.value
    }

    private suspend fun onChange(applied: PaymentTransactions.Applied) {
        val lending = Database {
            val link = SpaceLendingTransactions.selectAll()
                .where { SpaceLendingTransactions.transaction eq applied.transactionId }
                .firstOrNull() ?: return@Database null
            SpaceLendingEntity.findById(link[SpaceLendingTransactions.lending].value)
        } ?: return

        val changed = Database {
            when (applied.current) {
                PaymentStatus.COMPLETED -> when {
                    lending.paymentStatus == PaymentStatus.COMPLETED || lending.paymentStatus == PaymentStatus.REFUNDED -> {
                        // Paid twice (two payments started, or paid by hand meanwhile): the money has to be returned by a person
                        val message = "Lending ${lending.id.value} was already ${lending.paymentStatus} when transaction ${applied.transactionId} was paid"
                        logger.error(message)
                        Sentry.captureMessage(message)
                        false
                    }
                    else -> {
                        lending.paymentStatus = PaymentStatus.COMPLETED
                        true
                    }
                }
                PaymentStatus.REFUNDED -> {
                    lending.paymentStatus = PaymentStatus.REFUNDED
                    true
                }
                // A failed or cancelled attempt leaves the lending to pay: they can try again
                else -> false
            }
        }
        if (changed) lending.updated()
    }
}
